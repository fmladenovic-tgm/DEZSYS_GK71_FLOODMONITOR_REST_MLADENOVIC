/*
 * Flood Monitor Webclient (jQuery)
 *
 * Alle Daten werden ueber die REST-API geladen. Je nach gewaehltem Format wird der
 * Accept-Header auf application/json oder application/xml gesetzt. XML-Antworten werden
 * mit $.parseXML ausgewertet und in dieselben JavaScript-Objekte umgewandelt wie JSON,
 * damit die Darstellung fuer beide Formate identisch ist.
 */
$(function () {
    'use strict';

    const API = '/api/v1';
    const MIME = { json: 'application/json', xml: 'application/xml' };

    const state = {
        format: 'json',
        selectedId: null,
        stations: [],
        timer: null,
        pending: 0,
        refreshing: false
    };

    // ------------------------------------------------------------------ Hilfsfunktionen

    function esc(value) {
        return $('<div>').text(value == null ? '' : String(value)).html();
    }

    function fmt(value, digits) {
        if (value == null || value === '' || isNaN(value)) {
            return '–';
        }
        const d = digits == null ? 1 : digits;
        return Number(value).toLocaleString('de-AT', { minimumFractionDigits: d, maximumFractionDigits: d });
    }

    function fmtTime(iso) {
        if (!iso) {
            return '–';
        }
        const date = new Date(iso);
        return isNaN(date) ? iso : date.toLocaleString('de-AT', {
            day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit'
        });
    }

    function badge(value) {
        const v = value || 'UNKNOWN';
        return '<span class="badge ' + esc(v) + '">' + esc(v) + '</span>';
    }

    /** datetime-local (lokale Zeit) -> ISO-8601 UTC, z.B. 2026-05-01T08:00:00.000Z */
    function localToIso(value) {
        if (!value) {
            return undefined;
        }
        const date = new Date(value);
        return isNaN(date) ? value : date.toISOString();
    }

    function isoToLocalInput(date) {
        const pad = n => String(n).padStart(2, '0');
        return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + 'T'
            + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds());
    }

    function prettyXml(xml) {
        let indent = 0;
        return xml.replace(/>\s*</g, '>\n<').split('\n').map(function (line) {
            if (/^<\//.test(line)) {
                indent = Math.max(indent - 1, 0);
            }
            const out = '  '.repeat(indent) + line;
            if (/^<[^!?/][^>]*[^/]>$/.test(line) && !/<\/[^>]+>$/.test(line)) {
                indent++;
            }
            return out;
        }).join('\n');
    }

    // ------------------------------------------------------------------ XML -> Objekte

    function txt(el, name) {
        const child = $(el).children(name).first();
        return child.length ? child.text() : null;
    }

    function num(el, name) {
        const t = txt(el, name);
        return t == null || t === '' ? null : Number(t);
    }

    const xml = {
        station: function (el) {
            const loc = $(el).children('location').first();
            return {
                id: txt(el, 'id'),
                name: txt(el, 'name'),
                river: txt(el, 'river'),
                location: loc.length ? { latitude: num(loc, 'latitude'), longitude: num(loc, 'longitude') } : null,
                normalWaterLevel: num(el, 'normalWaterLevel'),
                warningWaterLevel: num(el, 'warningWaterLevel'),
                criticalWaterLevel: num(el, 'criticalWaterLevel'),
                isActive: txt(el, 'isActive') === 'true',
                status: txt(el, 'status'),
                currentWarningLevel: txt(el, 'currentWarningLevel')
            };
        },
        measurement: function (el) {
            return {
                stationId: txt(el, 'stationId'),
                timestamp: txt(el, 'timestamp'),
                waterLevel: num(el, 'waterLevel'),
                flowRate: num(el, 'flowRate'),
                rainfall: num(el, 'rainfall'),
                temperature: num(el, 'temperature'),
                batteryLevel: num(el, 'batteryLevel'),
                status: txt(el, 'status'),
                warningLevel: txt(el, 'warningLevel')
            };
        },
        alert: function (el) {
            return {
                stationId: txt(el, 'stationId'),
                stationName: txt(el, 'stationName'),
                river: txt(el, 'river'),
                timestamp: txt(el, 'timestamp'),
                warningLevel: txt(el, 'warningLevel'),
                waterLevel: num(el, 'waterLevel'),
                thresholdWaterLevel: num(el, 'thresholdWaterLevel'),
                message: txt(el, 'message')
            };
        },
        /** wandelt ein komplettes XML-Dokument anhand des Wurzelelements um */
        document: function (doc) {
            const root = doc.documentElement;
            const $root = $(root);
            switch (root.nodeName) {
                case 'stations':
                    return { count: num(root, 'count'), stations: $root.children('station').map((i, e) => xml.station(e)).get() };
                case 'station':
                    return xml.station(root);
                case 'measurement':
                    return xml.measurement(root);
                case 'measurements':
                    return {
                        stationId: txt(root, 'stationId'), count: num(root, 'count'),
                        measurements: $root.children('measurement').map((i, e) => xml.measurement(e)).get()
                    };
                case 'alerts':
                    return {
                        generatedAt: txt(root, 'generatedAt'), count: num(root, 'count'),
                        alerts: $root.children('alert').map((i, e) => xml.alert(e)).get()
                    };
                case 'statistics': {
                    const s = {};
                    ['stationId', 'from', 'to'].forEach(k => s[k] = txt(root, k));
                    ['measurementCount', 'minWaterLevel', 'maxWaterLevel', 'averageWaterLevel', 'averageFlowRate',
                        'totalRainfall', 'warningCount', 'criticalCount'].forEach(k => s[k] = num(root, k));
                    return s;
                }
                case 'error':
                    return { status: num(root, 'status'), error: txt(root, 'error'), message: txt(root, 'message'), path: txt(root, 'path') };
                default:
                    throw new Error('Unbekanntes XML-Element <' + root.nodeName + '>');
            }
        }
    };

    // ------------------------------------------------------------------ HTTP

    function setLoading(delta) {
        state.pending = Math.max(0, state.pending + delta);
        $('#loading').toggleClass('hidden', state.pending === 0);
    }

    function parseBody(text, contentType) {
        if (!text) {
            return null;
        }
        if (contentType && contentType.indexOf('xml') !== -1) {
            return xml.document($.parseXML(text));
        }
        return JSON.parse(text);
    }

    /**
     * GET-Request im gewaehlten Format. Liefert ein Promise mit dem geparsten Objekt.
     * Leere Parameter werden nicht mitgeschickt.
     */
    function apiGet(path, params) {
        const format = state.format;
        const query = {};
        $.each(params || {}, function (k, v) {
            if (v !== undefined && v !== null && v !== '') {
                query[k] = v;
            }
        });
        const deferred = $.Deferred();
        setLoading(1);
        $.ajax({
            url: API + path,
            data: query,
            method: 'GET',
            dataType: 'text',
            headers: { Accept: MIME[format] },
            timeout: 10000
        }).done(function (text, status, xhr) {
            try {
                const body = parseBody(text, xhr.getResponseHeader('Content-Type'));
                showRaw(this.url, format, text);
                deferred.resolve(body);
            } catch (e) {
                deferred.reject('Antwort konnte nicht verarbeitet werden: ' + e.message);
            }
        }).fail(function (xhr, textStatus) {
            deferred.reject(describeError(xhr, textStatus));
        }).always(function () {
            setLoading(-1);
        });
        return deferred.promise();
    }

    function describeError(xhr, textStatus) {
        if (textStatus === 'timeout') {
            return 'Zeitüberschreitung – der Server antwortet nicht.';
        }
        if (xhr.status === 0) {
            return 'Server nicht erreichbar. Läuft die Anwendung (./gradlew bootRun)?';
        }
        try {
            const body = parseBody(xhr.responseText, xhr.getResponseHeader('Content-Type'));
            if (body && body.message) {
                return body.status + ' ' + body.error + ': ' + body.message;
            }
        } catch (ignored) {
            // kein lesbarer Fehler-Body
        }
        return 'HTTP ' + xhr.status + ' ' + (xhr.statusText || '');
    }

    function showRaw(url, format, text) {
        $('#rawUrl').text(url);
        $('#rawFormat').text(format.toUpperCase());
        $('#rawResponse').text(format === 'xml' ? prettyXml(text) : JSON.stringify(JSON.parse(text), null, 2));
    }

    function showError(message) {
        $('#errorText').text(message);
        $('#errorBox').removeClass('hidden');
    }

    function clearError() {
        $('#errorBox').addClass('hidden');
    }

    // ------------------------------------------------------------------ Stationen

    function loadStations() {
        return apiGet('/stations', { river: $('#riverFilter').val() }).done(function (data) {
            state.stations = data.stations;
            // keine oder nicht mehr vorhandene Auswahl -> erste Station waehlen
            if (!data.stations.some(s => s.id === state.selectedId)) {
                state.selectedId = data.stations.length ? data.stations[0].id : null;
            }
            renderStations();
            renderStationHeader();
        });
    }

    function renderStations() {
        const $list = $('#stationList').empty();
        if (!state.stations.length) {
            $list.append('<li class="empty">Keine Stationen gefunden</li>');
        }
        state.stations.forEach(function (s) {
            const level = s.currentWarningLevel || 'UNKNOWN';
            $('<li>')
                .addClass(level)
                .toggleClass('active', s.id === state.selectedId)
                .attr('data-id', s.id)
                .html(
                    '<div class="row"><span class="name">' + esc(s.name) + '</span>' + badge(level) + '</div>' +
                    '<div class="row muted"><span>' + esc(s.id) + ' · ' + esc(s.river) + '</span>' +
                    '<span>' + (s.isActive ? esc(s.status) : 'deaktiviert') + '</span></div>')
                .appendTo($list);
        });
        // Fluss-Filter einmalig befuellen
        const $river = $('#riverFilter');
        if ($river.children().length === 1 && state.stations.length) {
            [...new Set(state.stations.map(s => s.river))].sort().forEach(function (r) {
                $river.append($('<option>').val(r).text(r));
            });
        }
    }

    function selectedStation() {
        return state.stations.find(s => s.id === state.selectedId);
    }

    function selectStation(id) {
        state.selectedId = id;
        renderStations();
        loadStationData();
    }

    // ------------------------------------------------------------------ aktueller Messwert

    function renderStationHeader() {
        const s = selectedStation();
        if (s) {
            $('#stationTitle').text(s.name + ' (' + s.id + ')');
            $('#stationMeta').html(esc(s.river) + ' · Warnwert ' + fmt(s.warningWaterLevel, 0) + ' cm · kritisch '
                + fmt(s.criticalWaterLevel, 0) + ' cm · ' + badge(s.status));
        }
    }

    function loadLatest() {
        renderStationHeader();
        // Fehler (z.B. 404 "keine Messwerte" bei deaktivierter Station) nur lokal anzeigen
        return apiGet('/stations/' + encodeURIComponent(state.selectedId) + '/measurements/latest')
            .then(renderCurrent, function (msg) {
                $('#current').html('<div class="empty">' + esc(msg) + '</div>');
            });
    }

    function tile(label, value, unit, cls) {
        return '<div class="tile ' + (cls || '') + '"><div class="label">' + esc(label) + '</div>' +
            '<div class="value">' + value + (unit ? ' <span class="unit">' + esc(unit) + '</span>' : '') + '</div></div>';
    }

    function renderCurrent(m) {
        $('#current').html(
            tile('Warnstufe', badge(m.warningLevel), '', m.warningLevel) +
            tile('Wasserstand', fmt(m.waterLevel), 'cm', m.warningLevel) +
            tile('Durchfluss', fmt(m.flowRate), 'm³/s') +
            tile('Niederschlag', fmt(m.rainfall), 'mm/h') +
            tile('Temperatur', fmt(m.temperature), '°C') +
            tile('Akku', fmt(m.batteryLevel, 0), '%') +
            tile('Status', badge(m.status)) +
            tile('Zeitpunkt', '<span style="font-size:13px">' + esc(fmtTime(m.timestamp)) + '</span>')
        );
    }

    // ------------------------------------------------------------------ Historie & Diagramm

    function historyParams() {
        return {
            from: localToIso($('#from').val()),
            to: localToIso($('#to').val()),
            warningLevel: $('#warningLevel').val(),
            limit: $('#limit').val()
        };
    }

    function loadHistory() {
        const $body = $('#historyTable tbody');
        if (!$body.children().length) {
            $body.html('<tr><td colspan="8" class="empty">Lade Messwerte…</td></tr>');
        }
        return apiGet('/stations/' + encodeURIComponent(state.selectedId) + '/measurements', historyParams())
            .done(function (data) {
                renderHistory(data.measurements);
                renderChart(data.measurements);
            })
            .fail(function (msg) {
                $body.html('<tr><td colspan="8" class="empty">' + esc(msg) + '</td></tr>');
                $('#chart').empty();
                $('#historyCount').text('');
            });
    }

    function renderHistory(list) {
        const s = selectedStation();
        const name = s ? s.name : state.selectedId;
        const $body = $('#historyTable tbody').empty();
        $('#historyCount').text(list.length + ' Messwerte');
        if (!list.length) {
            $body.html('<tr><td colspan="8" class="empty">Keine Messwerte für diesen Filter</td></tr>');
            return;
        }
        const rows = list.map(function (m) {
            return '<tr class="' + esc(m.warningLevel) + '">' +
                '<td>' + esc(name) + ' <span class="muted">' + esc(m.stationId) + '</span></td>' +
                '<td>' + esc(fmtTime(m.timestamp)) + '</td>' +
                '<td class="num">' + fmt(m.waterLevel) + '</td>' +
                '<td class="num">' + fmt(m.flowRate) + '</td>' +
                '<td class="num">' + fmt(m.rainfall) + '</td>' +
                '<td class="num">' + fmt(m.batteryLevel, 0) + '</td>' +
                '<td>' + badge(m.status) + '</td>' +
                '<td>' + badge(m.warningLevel) + '</td></tr>';
        });
        $body.html(rows.join(''));
    }

    function renderChart(list) {
        const s = selectedStation();
        const $chart = $('#chart');
        if (!s || list.length < 2) {
            $chart.empty();
            return;
        }
        const data = list.slice().reverse(); // chronologisch
        const W = 900, H = 220, L = 46, R = 10, T = 10, B = 24;
        const times = data.map(m => new Date(m.timestamp).getTime());
        const levels = data.map(m => m.waterLevel);
        const minT = times[0], maxT = times[times.length - 1] || minT + 1;
        let minY = Math.min(...levels, s.warningWaterLevel) - 10;
        let maxY = Math.max(...levels, s.criticalWaterLevel) + 10;
        minY = Math.max(0, minY);
        const x = t => L + (W - L - R) * (t - minT) / Math.max(1, maxT - minT);
        const y = v => T + (H - T - B) * (1 - (v - minY) / Math.max(1, maxY - minY));

        const points = data.map((m, i) => x(times[i]).toFixed(1) + ',' + y(m.waterLevel).toFixed(1));
        let svg = '<svg viewBox="0 0 ' + W + ' ' + H + '" preserveAspectRatio="none" role="img" aria-label="Wasserstandsverlauf">';
        for (let i = 0; i <= 4; i++) {
            const v = minY + (maxY - minY) * i / 4;
            svg += '<line class="grid" x1="' + L + '" x2="' + (W - R) + '" y1="' + y(v) + '" y2="' + y(v) + '"/>' +
                '<text x="4" y="' + (y(v) + 4) + '">' + Math.round(v) + '</text>';
        }
        svg += '<polygon class="area" points="' + x(minT) + ',' + y(minY) + ' ' + points.join(' ') + ' ' + x(maxT) + ',' + y(minY) + '"/>';
        svg += '<line class="threshold warning" x1="' + L + '" x2="' + (W - R) + '" y1="' + y(s.warningWaterLevel) + '" y2="' + y(s.warningWaterLevel) + '"/>';
        svg += '<line class="threshold critical" x1="' + L + '" x2="' + (W - R) + '" y1="' + y(s.criticalWaterLevel) + '" y2="' + y(s.criticalWaterLevel) + '"/>';
        svg += '<polyline class="line" points="' + points.join(' ') + '"/>';
        svg += '<text x="' + L + '" y="' + (H - 6) + '">' + esc(fmtTime(data[0].timestamp)) + '</text>';
        svg += '<text x="' + (W - R) + '" y="' + (H - 6) + '" text-anchor="end">' + esc(fmtTime(data[data.length - 1].timestamp)) + '</text>';
        svg += '</svg>';
        $chart.html('<div class="muted">Wasserstand (cm) – <span style="color:var(--warning)">Warnwert</span>, ' +
            '<span style="color:var(--critical)">kritischer Wert</span></div>' + svg);
    }

    // ------------------------------------------------------------------ Statistik

    function loadStatistics() {
        const p = historyParams();
        return apiGet('/stations/' + encodeURIComponent(state.selectedId) + '/statistics', { from: p.from, to: p.to })
            .done(function (st) {
                $('#statistics').html(
                    tile('Messungen', fmt(st.measurementCount, 0)) +
                    tile('Min. Wasserstand', fmt(st.minWaterLevel), 'cm') +
                    tile('Max. Wasserstand', fmt(st.maxWaterLevel), 'cm') +
                    tile('Ø Wasserstand', fmt(st.averageWaterLevel), 'cm') +
                    tile('Ø Durchfluss', fmt(st.averageFlowRate), 'm³/s') +
                    tile('Σ Niederschlag', fmt(st.totalRainfall), 'mm') +
                    tile('Warnungen', fmt(st.warningCount, 0), '', st.warningCount ? 'WARNING' : '') +
                    tile('Kritisch', fmt(st.criticalCount, 0), '', st.criticalCount ? 'CRITICAL' : ''));
            })
            .fail(function (msg) {
                $('#statistics').html('<div class="empty">' + esc(msg) + '</div>');
            });
    }

    // ------------------------------------------------------------------ Warnungen

    function loadAlerts() {
        return apiGet('/alerts').done(function (data) {
            const $alerts = $('#alerts').empty();
            data.alerts.forEach(function (a) {
                $('<div>').addClass('alert ' + a.warningLevel).html(
                    '<span>' + badge(a.warningLevel) + ' ' + esc(a.message) + '</span>' +
                    '<span class="muted">' + esc(fmtTime(a.timestamp)) + '</span>').appendTo($alerts);
            });
        });
    }

    // ------------------------------------------------------------------ Aktualisierung

    function loadStationData() {
        if (!state.selectedId) {
            $('#current, #chart, #statistics, #historyTable tbody, #stationMeta, #historyCount').empty();
            $('#stationTitle').text('Station wählen');
            return $.when();
        }
        return $.when(loadLatest(), loadHistory(), loadStatistics());
    }

    function refreshAll() {
        if (state.refreshing) {
            return; // vorherige Aktualisierung laeuft noch
        }
        state.refreshing = true;
        // zuerst Stationen (Namen/Grenzwerte werden fuer Tabelle und Diagramm benoetigt)
        loadStations()
            .then(function () {
                return $.when(loadAlerts(), loadStationData());
            })
            .done(function () {
                clearError();
                $('#lastUpdate').text('Stand: ' + new Date().toLocaleTimeString('de-AT'));
            })
            .fail(showError)
            .always(function () {
                state.refreshing = false;
            });
    }

    function restartTimer() {
        clearInterval(state.timer);
        state.timer = null;
        if ($('#autoRefresh').is(':checked')) {
            state.timer = setInterval(refreshAll, Number($('#refreshInterval').val()) * 1000);
        }
    }

    // ------------------------------------------------------------------ Events

    $('input[name=format]').on('change', function () {
        state.format = this.value;
        refreshAll();
    });

    $('#stationList').on('click', 'li[data-id]', function () {
        selectStation($(this).data('id'));
    });

    $('#riverFilter').on('change', function () {
        loadStations().then(loadStationData).done(clearError).fail(showError);
    });

    $('#filterForm').on('submit', function (e) {
        e.preventDefault();
        $.when(loadHistory(), loadStatistics()).done(clearError).fail(showError);
    });

    $('#resetFilter').on('click', function () {
        $('#from, #to').val('');
        $('#warningLevel').val('');
        $('#limit').val(50);
        $('#filterForm').trigger('submit');
    });

    $('.quick button').on('click', function () {
        const now = new Date();
        $('#from').val(isoToLocalInput(new Date(now.getTime() - $(this).data('hours') * 3600 * 1000)));
        $('#to').val('');
        $('#filterForm').trigger('submit');
    });

    $('#autoRefresh, #refreshInterval').on('change', restartTimer);
    $('#refreshNow').on('click', refreshAll);
    $('#errorClose').on('click', clearError);

    // Startzustand optional aus der URL, z.B. index.html?format=xml&station=ST-003
    const params = new URLSearchParams(window.location.search);
    if (params.get('format') === 'xml') {
        state.format = 'xml';
        $('input[name=format][value=xml]').prop('checked', true);
    }
    if (params.get('station')) {
        state.selectedId = params.get('station');
    }
    if (params.get('raw')) {
        $('details.raw').attr('open', true);
    }

    refreshAll();
    restartTimer();
});
