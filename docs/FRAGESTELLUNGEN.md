# Fragestellungen

Die Zahlen in [ ] sind die Quellen ganz unten.

## 1. Was sind die Hauptmerkmale von JSON?

- textbasiertes, leichtgewichtiges Datenformat, unabhängig von der Programmiersprache [1][2]
- Datentypen: Objekt `{}`, Array `[]`, String, Zahl, `true`/`false`, `null` [1]
- kein eigener Datumstyp → Zeitpunkte als ISO-8601-String, z. B. `"2026-05-01T10:15:00Z"`
- beim Austausch zwischen Systemen muss UTF-8 verwendet werden [1, Kap. 8.1]
- keine Kommentare, kein eingebautes Schema
- gut lesbar und wenig Overhead, deshalb das Standardformat bei Web-APIs
- Medientyp `application/json`

## 2. Was sind die Hauptmerkmale von XML?

- Auszeichnungssprache vom W3C, Daten stehen in Elementen (Tags) und Attributen [3]
- muss wohlgeformt sein: genau ein Wurzelelement, alle Tags richtig geschlossen und verschachtelt [3]
- erweiterbar, eigene Tag-Namen möglich, Namespaces gegen Namenskonflikte [4]
- kann mit DTD oder XML Schema (XSD) geprüft werden [3][5]
- alle Werte sind Text, Typen nur über ein Schema
- mehr Overhead als JSON (Start- und End-Tag), dafür viele Tools (XPath, XSLT)
- Medientyp `application/xml` [6]

## 3. Wie unterscheidet die Applikation, ob JSON oder XML zurückkommt?

Über den HTTP-Header `Accept` (Content Negotiation [7, Kap. 12.5.1]):

- `Accept: application/json` → JSON, `Accept: application/xml` → XML
- Die Controller haben `produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE}`.
- Spring schaut sich den Accept-Header an und nimmt den passenden Message Converter: Jackson für JSON und
  Jackson XML für XML, weil `jackson-dataformat-xml` als Abhängigkeit drin ist [8][9].
- In `WebConfig` ist eingestellt, dass ohne Accept-Header JSON kommt.
- Bei einem anderen Format (z. B. `text/csv`) gibt es eine `HttpMediaTypeNotAcceptableException`, daraus
  macht der `GlobalExceptionHandler` einen 406.
- Fehler kommen auch als XML, wenn im Accept-Header XML steht.

## 4. Was zeichnet ein REST-Interface aus und warum wird es so oft verwendet?

REST kommt von Roy Fielding (Dissertation 2000) [10]. Die wichtigsten Regeln:

- **Client-Server**: Client und Server sind getrennt
- **Stateless**: jede Anfrage hat alle Infos, der Server merkt sich keine Session
- **Cacheable**: Antworten können gecacht werden
- **Uniform Interface**: Ressourcen haben eine URI (z. B. `/api/v1/stations/ST-001`), man arbeitet mit
  Repräsentationen (JSON/XML) und den normalen HTTP-Methoden und Statuscodes
- **Layered System**: Proxies oder Load Balancer können dazwischen sein

Warum so beliebt: läuft über normales HTTP (funktioniert überall, auch durch Firewalls), ist einfach, egal
welche Programmiersprache, skaliert gut weil stateless, und es gibt viele Tools (z. B. OpenAPI/Swagger).

## 5. Unterschied zwischen GET, POST, PUT und PATCH

| Methode | Wofür | sicher | idempotent | Beispiel |
|---|---|---|---|---|
| GET | Daten lesen | ja | ja | `GET /api/v1/stations` |
| POST | etwas Neues anlegen | nein | nein | `POST /api/v1/stations` → 201 |
| PUT | Ressource komplett ersetzen | nein | ja | `PUT /api/v1/stations/ST-001` mit allen Feldern |
| PATCH | Ressource teilweise ändern | nein | nicht unbedingt | `PATCH /api/v1/stations/ST-001` mit `{"isActive":false}` |

- *sicher* = ändert nichts am Server [7, Kap. 9.2.1]
- *idempotent* = mehrmals ausführen hat den gleichen Effekt wie einmal [7, Kap. 9.2.2]. Zweimal POST legt
  zweimal an, zweimal PUT mit den gleichen Daten ändert nichts mehr.
- PATCH ist in RFC 5789 definiert [11].

## 6. Neuer Pfad / neue Methode – was muss man ändern?

Beispiel `GET /api/v1/rivers`:

1. **DTO/Model**: neue Klasse für die Antwort, mit `@JacksonXmlRootElement` (sonst stimmt das XML nicht)
2. **Repository**: nur wenn man neue Daten braucht
3. **Service**: die Logik, Parameter prüfen
4. **Controller**: neue Methode mit `@GetMapping("/rivers")` (oder `@PostMapping`, ...) und `produces = {JSON, XML}`
5. **Fehler**: neue Fehlerfälle mit `ApiException` werfen, der `GlobalExceptionHandler` macht den Rest
6. **Sicherheit**: bei schreibenden Methoden prüft der `ApiKeyInterceptor` automatisch den Key
7. **OpenAPI**: `@Operation` und `@ApiResponse` dazuschreiben
8. **Tests** dazu
9. **Webclient** (`app.js`): neuen Aufruf einbauen
10. **README** aktualisieren

## 7. Kann man OpenAPI und Swagger vergleichen?

Nur bedingt, weil das eine ein Standard ist und das andere Tools dafür.

- Früher hieß die Spezifikation „Swagger Specification". 2015 wurde sie an die OpenAPI Initiative (Linux
  Foundation) übergeben und in **OpenAPI Specification** umbenannt [12][13].
- **OpenAPI** = die Spezifikation, also wie man eine REST-API in JSON/YAML beschreibt (Endpunkte, Parameter,
  Bodies, Statuscodes, Authentifizierung) [14].
- **Swagger** = die Tools von SmartBear, die mit OpenAPI arbeiten: Swagger UI, Swagger Editor, Swagger Codegen [12].

In meinem Projekt erzeugt springdoc das OpenAPI-Dokument (`/v3/api-docs`, `docs/openapi.yaml`) und Swagger UI
(`/swagger-ui.html`) zeigt es an.

## 8. Wo liegt der Fehler im Code-Snippet?

```java
@GetMapping("/stations/add")
public String createStation(@RequestParam String name, @RequestParam double warning) {
    stationService.save(new Station(name, warning));
    return "Station successfully created!";
}
```

Der Hauptfehler: **Es wird mit GET etwas angelegt.** GET ist laut HTTP eine sichere Methode und darf nichts am
Server ändern [7, Kap. 9.2.1]. Wenn z. B. ein Crawler, der Browser (Prefetch) oder ein Cache die URL aufruft,
wird ungewollt eine Station angelegt. Außerdem könnte man das mit einem einfachen Link auslösen (CSRF).

Weitere Probleme:
- `/add` ist ein Verb in der URL, bei REST sollte die URL eine Ressource sein und die Methode sagt was passiert → `POST /api/v1/stations`
- Daten kommen als Query-Parameter statt im Body (landen in Logs und im Browserverlauf)
- Antwort ist 200 mit einem Text statt **201 Created** mit der angelegten Station und `Location`-Header [7, Kap. 15.3.2]
- keine Prüfung der Eingaben (leerer Name, falsche Grenzwerte), keine ID, kein API-Key

Richtig (so wie in meinem `StationController`):

```java
@PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public ResponseEntity<Station> createStation(@RequestBody Station station) {
    Station created = stationService.createStation(station);
    return ResponseEntity.created(URI.create("/api/v1/stations/" + created.getId())).body(created);
}
```

## Quellen

1. RFC 8259 – The JavaScript Object Notation (JSON) Data Interchange Format, IETF 2017. https://www.rfc-editor.org/rfc/rfc8259
2. ECMA-404 – The JSON Data Interchange Syntax. https://ecma-international.org/publications-and-standards/standards/ecma-404/
3. W3C: Extensible Markup Language (XML) 1.0 (Fifth Edition). https://www.w3.org/TR/xml/
4. W3C: Namespaces in XML 1.0. https://www.w3.org/TR/xml-names/
5. W3C: XML Schema Part 1: Structures. https://www.w3.org/TR/xmlschema-1/
6. RFC 7303 – XML Media Types. https://www.rfc-editor.org/rfc/rfc7303
7. RFC 9110 – HTTP Semantics, IETF 2022. https://www.rfc-editor.org/rfc/rfc9110
8. Spring Framework Doku: Content Negotiation. https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/content-negotiation.html
9. Spring Boot Doku: Spring MVC / HttpMessageConverters. https://docs.spring.io/spring-boot/reference/web/servlet.html
10. R. Fielding: Architectural Styles and the Design of Network-based Software Architectures, Kapitel 5, 2000. https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm
11. RFC 5789 – PATCH Method for HTTP. https://www.rfc-editor.org/rfc/rfc5789
12. SmartBear: What Is the Difference Between Swagger and OpenAPI? https://swagger.io/blog/api-strategy/difference-between-swagger-and-openapi/
13. OpenAPI Initiative FAQ. https://www.openapis.org/faq
14. OpenAPI Specification. https://spec.openapis.org/oas/latest.html
