# Cheatsheet – Flood Monitor REST (GK 7.1)

## Starten
```bash
./gradlew bootRun     # PowerShell: .\gradlew.bat bootRun
./gradlew test        # 54 Tests
```
Client `http://localhost:8080/` · Swagger `/swagger-ui.html` · OpenAPI `/v3/api-docs`

## Architektur (Pakete unter `com.example.floodmonitor`)
`model` (Station, Location, Measurement, WarningLevel, StationStatus) → `repository` (In-Memory, Seed:
5 Stationen) → `service` (StationService, MeasurementService, AlertService, WarningLevelCalculator,
QueryParameterValidator, MeasurementSimulator, SimulationService/-Scheduler) → `controller`
(Station-, Measurement-, AlertController) · `exception` (ApiException + GlobalExceptionHandler) ·
`security` (ApiKeyInterceptor) · `config` (WebConfig, OpenApiConfig, AppConfig, SecurityHeadersFilter)

## Endpunkte `/api/v1`
| | |
|---|---|
| `GET /stations?river=&isActive=&warningLevel=` | Stationen + `currentWarningLevel` |
| `GET /stations/{id}` | Station / 404 |
| `GET /stations/{id}/measurements/latest` | letzter Messwert |
| `GET /stations/{id}/measurements?from=&to=&limit=&warningLevel=` | Historie, neueste zuerst, limit 1–1000 (100) |
| `GET /stations/{id}/statistics?from=&to=` | min/max/avg Pegel, avg Durchfluss, Σ Regen, Anzahl, Warnungen, kritisch |
| `GET /alerts?warningLevel=&river=` | aktuelle WARNING/CRITICAL |
| `POST /stations` · `PATCH /stations/{id}` · `DELETE /stations/{id}` | Header `X-API-Key` → 201 / 200 / 204 |

## JSON vs. XML
- `Accept: application/json` (Standard, auch ohne Header) · `Accept: application/xml` · sonst **406**
- Controller: `produces = {JSON, XML}`; Converter: Jackson JSON / Jackson XML (`jackson-dataformat-xml`)
- `WebConfig`: nur Accept-Header, `defaultContentType(JSON, ALL)`, XML-Deklaration an
- XML-Namen: `@JacksonXmlRootElement`, Listen: `@JacksonXmlElementWrapper(useWrapping=false)`

## Warnstufe (serverseitig, `WarningLevelCalculator`)
`≥ kritisch → CRITICAL` · `≥ Warnwert → WARNING` · `≥ 90 % Warnwert UND (Anstieg ≥ 10 cm ODER Regen ≥ 20 mm/h) → WARNING` · sonst `NORMAL` ·
ungültig / keine Messung / > 120 s alt / inaktiv / OFFLINE → `UNKNOWN`

## Simulation (`MeasurementSimulator`, aus vorherigem Wert)
- Regen: Start 4 %, Ende 7 %, Random Walk 0,2–60 mm/h
- Pegel: `+0,35·Regen − 0,04·(Pegel − Basis) ± Rauschen`, max ±25 cm/Schritt, ≥ 0
- Durchfluss: `Q_mittel·(W/W_normal)^1,6` (monoton)
- Akku: −1 % mit 12 %; < 15 % → MAINTENANCE; 0 % → OFFLINE (keine Messung); Tausch 3 % / Reparatur 2 %
- Intervall `simulation.interval=10000`, abschaltbar `simulation.enabled=false`, Seed `simulation.seed`

## Statuscodes
200 OK · 201 Created (+Location) · 204 DELETE · 400 Parameter/Body · 401 kein/falscher Key · 403 Lese-Key ·
404 Station/Messwert · 405 Methode · 406 Format · 409 ID existiert · 415 Content-Type · 500 generisch.
Format: `{timestamp, status, error, message, path}` als JSON oder XML, nie Stacktraces.

## Tests deterministisch
Profil `test`: Simulation aus, `TestClockConfig` (feste Uhr 2026-05-01T12:00Z), Testdaten in `@BeforeEach`,
Simulator mit Seed / festem `Random`.

## Fragestellungen – Kurzantworten
1. **JSON**: leichtgewichtig, Objekt/Array/String/Number/Boolean/null, UTF-8, kein Schema/Kommentar (RFC 8259)
2. **XML**: Elemente+Attribute, wohlgeformt, Namespaces, DTD/XSD, alles Text, mehr Overhead (W3C XML 1.0)
3. **Accept-Header** → ContentNegotiationManager → passender HttpMessageConverter; sonst 406
4. **REST** (Fielding 2000): Client-Server, stateless, cacheable, uniform interface, layered; HTTP-basiert, einfach, skalierbar
5. **GET** lesen (safe, idempotent) · **POST** anlegen (nicht idempotent, 201) · **PUT** ganz ersetzen (idempotent) · **PATCH** teilweise (RFC 9110/5789)
6. Neuer Pfad: DTO/Model → Service → Controller (`@GetMapping`) → Exception → OpenAPI → Tests → Client (`xml.document`) → Doku
7. **OpenAPI** = Spezifikation (OAI/Linux Foundation), **Swagger** = Tools (SmartBear) – Standard vs. Werkzeug
8. Snippet: **GET ändert Zustand** (nicht safe → Crawler/Prefetch/CSRF), Verb in URI, Query statt Body, 200+String statt 201+Location+Ressource, keine Validierung → `POST /api/v1/stations`
