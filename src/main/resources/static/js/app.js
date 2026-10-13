// Webclient fuer die Flood Monitor API

let format = 'json';
let selectedStation = null;
let stationNames = {};
let timer = null;

// GET-Request im gewaehlten Format, gibt immer ein JS-Objekt zurueck
function apiGet(url, params) {
    $('#loading').show();
    return $.ajax({
        url: url,
        data: params,
        headers: {Accept: format === 'xml' ? 'application/xml' : 'application/json'},
        dataType: 'text'
    }).then(function (text) {
        if (format === 'xml') {
            return xmlToObject($.parseXML(text).documentElement);
        }
        return JSON.parse(text);
    }).always(function () {
        $('#loading').hide();
    });
}

// wandelt XML in ein Objekt um, damit der Rest gleich bleibt wie bei JSON
// <stations><count>3</count><station>..</station><station>..</station></stations>
// -> {count: 3, stations: [{..}, {..}]}
function xmlToObject(node) {
    let children = $(node).children();
    if (children.length === 0) {
        let text = $(node).text();
        if (text !== '' && !isNaN(text)) return Number(text);
        if (text === 'true') return true;
        if (text === 'false') return false;
        return text;
    }
    let obj = {};
    children.each(function () {
        let name = this.nodeName;
        let value = xmlToObject(this);
        // Listen: station -> stations, measurement -> measurements, alert -> alerts
        if (name === 'station' || name === 'measurement' || name === 'alert') {
            let listName = name + 's';
            if (!obj[listName]) obj[listName] = [];
            obj[listName].push(value);
        } else {
            obj[name] = value;
        }
    });
    // leere Listen
    if (node.nodeName === 'stations' && !obj.stations) obj.stations = [];
    if (node.nodeName === 'measurements' && !obj.measurements) obj.measurements = [];
    if (node.nodeName === 'alerts' && !obj.alerts) obj.alerts = [];
    return obj;
}

// Fehlermeldung aus der Antwort holen (JSON oder XML)
function showError(xhr) {
    let msg = 'Server nicht erreichbar';
    if (xhr && xhr.status) {
        msg = 'Fehler ' + xhr.status;
        try {
            let text = xhr.responseText;
            let err = text.trim().startsWith('<') ? xmlToObject($.parseXML(text).documentElement) : JSON.parse(text);
            msg = err.status + ' ' + err.error + ': ' + err.message;
        } catch (e) {
            // kein lesbarer Fehler
        }
    }
    $('#error').text(msg).show();
}

function formatTime(iso) {
    return new Date(iso).toLocaleString('de-AT');
}

function loadStations() {
    return apiGet('/api/v1/stations').then(function (data) {
        let list = $('#stations').empty();
        data.stations.forEach(function (s) {
            stationNames[s.id] = s.name;
            let li = $('<li>').addClass(s.currentWarningLevel).attr('data-id', s.id);
            li.append($('<b>').text(s.name));
            li.append($('<div>').text(s.id + ' - ' + s.river + ' - ' + s.currentWarningLevel + (s.isActive ? '' : ' (deaktiviert)')));
            if (s.id === selectedStation) li.addClass('selected');
            list.append(li);
        });
        if (!selectedStation && data.stations.length > 0) {
            selectedStation = data.stations[0].id;
            $('#stations li').first().addClass('selected');
        }
    });
}

function loadAlerts() {
    return apiGet('/api/v1/alerts').then(function (data) {
        let box = $('#alerts').empty();
        data.alerts.forEach(function (a) {
            box.append($('<div>').addClass('alert ' + a.warningLevel).text(a.message));
        });
    });
}

function loadCurrent() {
    $('#stationName').text('Aktueller Messwert: ' + stationNames[selectedStation]);
    return apiGet('/api/v1/stations/' + selectedStation + '/measurements/latest').then(function (m) {
        $('#current').attr('class', m.warningLevel).html(
            'Zeit: ' + formatTime(m.timestamp) + '<br>' +
            'Wasserstand: <b>' + m.waterLevel + ' cm</b><br>' +
            'Durchfluss: ' + m.flowRate + ' m³/s<br>' +
            'Niederschlag: ' + m.rainfall + ' mm/h<br>' +
            'Temperatur: ' + m.temperature + ' °C<br>' +
            'Akku: ' + m.batteryLevel + ' %<br>' +
            'Status: ' + m.status + '<br>' +
            'Warnstufe: <b>' + m.warningLevel + '</b>');
    }, function (xhr) {
        // z.B. deaktivierte Station hat keine Messwerte -> nur hier anzeigen
        $('#current').attr('class', 'UNKNOWN').text('Keine Messwerte vorhanden');
        return $.Deferred().resolve();
    });
}

function loadHistory() {
    let params = {limit: $('#limit').val()};
    // datetime-local ist lokale Zeit -> in ISO (UTC) umwandeln
    if ($('#from').val()) params.from = new Date($('#from').val()).toISOString();
    if ($('#to').val()) params.to = new Date($('#to').val()).toISOString();
    if ($('#warningLevel').val()) params.warningLevel = $('#warningLevel').val();

    return apiGet('/api/v1/stations/' + selectedStation + '/measurements', params).then(function (data) {
        let tbody = $('#history').empty();
        if (data.measurements.length === 0) {
            tbody.append('<tr><td colspan="8">Keine Messwerte gefunden</td></tr>');
        }
        data.measurements.forEach(function (m) {
            let tr = $('<tr>');
            tr.append($('<td>').text(stationNames[m.stationId] || m.stationId));
            tr.append($('<td>').text(formatTime(m.timestamp)));
            tr.append($('<td>').text(m.waterLevel));
            tr.append($('<td>').text(m.flowRate));
            tr.append($('<td>').text(m.rainfall));
            tr.append($('<td>').text(m.batteryLevel));
            tr.append($('<td>').text(m.status));
            tr.append($('<td>').addClass(m.warningLevel).text(m.warningLevel));
            tbody.append(tr);
        });
    });
}

function refresh() {
    loadStations()
        .then(function () {
            return $.when(loadAlerts(), loadCurrent(), loadHistory());
        })
        .done(function () {
            $('#error').hide();
            $('#lastUpdate').text('Stand: ' + new Date().toLocaleTimeString('de-AT'));
        })
        .fail(showError);
}

function startTimer() {
    clearInterval(timer);
    if ($('#autoRefresh').is(':checked')) {
        timer = setInterval(refresh, 10000);
    }
}

$(function () {
    $('input[name=format]').change(function () {
        format = this.value;
        refresh();
    });

    $('#stations').on('click', 'li', function () {
        selectedStation = $(this).data('id');
        $('#stations li').removeClass('selected');
        $(this).addClass('selected');
        refresh();
    });

    $('#filter').submit(function (e) {
        e.preventDefault();
        loadHistory().done(function () { $('#error').hide(); }).fail(showError);
    });

    $('#refresh').click(refresh);
    $('#autoRefresh').change(startTimer);

    refresh();
    startTimer();
});
