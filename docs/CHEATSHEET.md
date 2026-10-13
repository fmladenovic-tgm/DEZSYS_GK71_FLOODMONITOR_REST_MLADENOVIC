# Cheatsheet Flood Monitor REST

**Starten:** `.\gradlew.bat bootRun` → http://localhost:8080/ · Swagger: `/swagger-ui.html` · Tests: `.\gradlew.bat test`

**Aufbau:** model → repository (im Speicher) → service (Logik, Simulation, Warnstufe) → controller (nur HTTP) ·
exception (ApiException + GlobalExceptionHandler) · config (WebConfig, ApiKeyInterceptor, OpenApiConfig) · static (Webclient)

**Endpunkte** `/api/v1`:
- `GET /stations?river=&isActive=&warningLevel=`
- `GET /stations/{id}`
- `GET /stations/{id}/measurements/latest`
- `GET /stations/{id}/measurements?from=&to=&limit=&warningLevel=` (limit 1-1000, neueste zuerst)
- `GET /stations/{id}/statistics?from=&to=`
- `GET /alerts?warningLevel=&river=`
- `POST /stations`, `DELETE /stations/{id}` mit Header `X-API-Key`

**JSON/XML:** über `Accept`-Header. Controller haben `produces = {JSON, XML}`, Jackson macht JSON bzw. XML
(`jackson-dataformat-xml`). Standard JSON, anderes Format → 406. XML-Namen über `@JacksonXmlRootElement`.

**Warnstufe:** ≥ kritisch → CRITICAL, ≥ Warnwert → WARNING, ≥ 90 % Warnwert + (Anstieg ≥ 10 cm oder Regen ≥ 20 mm/h) → WARNING,
sonst NORMAL. Keine/alte Messung (> 2 min), deaktiviert, OFFLINE → UNKNOWN.

**Simulation:** neuer Wert aus altem. Regen erhöht Pegel (+0,35 × Regen), sonst zurück zum Basispegel, max ±25 cm.
Durchfluss = mittel × (Pegel/normal)^1,6. Akku −1 % (12 %), < 15 % MAINTENANCE, 0 % OFFLINE. Intervall `simulation.interval`.

**Statuscodes:** 200, 201 (POST), 204 (DELETE), 400 (Parameter), 401 (kein Key), 403 (Lese-Key), 404, 406, 409 (ID doppelt), 415, 500.
Fehler immer `{timestamp, status, error, message, path}`, keine Stacktraces.

**Tests:** Simulation aus + feste Uhr → nicht zufällig.

**Fragen kurz:**
1. JSON: leicht, Objekt/Array/String/Zahl/Boolean/null, UTF-8 (RFC 8259)
2. XML: Tags + Attribute, wohlgeformt, Namespaces, XSD, mehr Overhead (W3C XML 1.0)
3. Accept-Header → Spring wählt den passenden Converter, sonst 406
4. REST (Fielding): Client-Server, stateless, cacheable, einheitliche Schnittstelle; einfach, läuft über HTTP
5. GET lesen, POST anlegen, PUT ganz ersetzen, PATCH teilweise ändern (RFC 9110, RFC 5789)
6. Neuer Pfad: DTO → Service → Controller → Fehler → OpenAPI → Tests → Client
7. OpenAPI = Standard, Swagger = Tools dafür
8. Snippet: GET ändert Daten (nicht safe), Verb in URL, Query statt Body, 200 + Text statt 201 → `POST /api/v1/stations`
