# Fragestellungen (Punkt 12)

Alle Antworten mit Quellverweis. Die Nummern in eckigen Klammern verweisen auf das
[Quellenverzeichnis](#quellen) am Ende.

---

## 1. Was sind die Hauptmerkmale von JSON?

**JSON (JavaScript Object Notation)** ist ein textbasiertes, sprachunabhängiges Datenaustauschformat,
standardisiert in RFC 8259 [1] und ECMA-404 [2].

- **Leichtgewichtig und gut lesbar** – wenig Overhead, keine schließenden Tags.
- **Wenige Datentypen:** Objekt `{}` (ungeordnete Schlüssel/Wert-Paare), Array `[]` (geordnete Liste),
  String, Number, `true`/`false`, `null` [1, Kap. 3–7].
- **Keine eigenen Typen für Datum oder Binärdaten** – Zeitpunkte werden z. B. als ISO-8601-String
  übertragen (`"2026-05-01T10:15:00Z"`, so auch in dieser API).
- **Kodierung:** Beim Austausch zwischen Systemen muss UTF-8 verwendet werden [1, Kap. 8.1].
- **Keine Kommentare, keine Namespaces, kein eingebautes Schema** – Validierung nur über externe
  Standards wie JSON Schema.
- **Direkte Abbildung auf Datenstrukturen** von JavaScript, Java (Jackson), Python usw. – deshalb das
  De-facto-Standardformat für Web-APIs.
- Medientyp: `application/json` [1, Kap. 11].

## 2. Was sind die Hauptmerkmale von XML?

**XML (Extensible Markup Language)** ist eine vom W3C standardisierte Auszeichnungssprache [3].

- **Baumstruktur aus Elementen** (`<waterLevel>318.4</waterLevel>`) mit optionalen **Attributen**.
- **Wohlgeformtheit:** genau ein Wurzelelement, korrekt verschachtelte und geschlossene Tags,
  case-sensitive [3, Kap. 2.1].
- **Selbstbeschreibend und erweiterbar** – eigene Elementnamen frei definierbar.
- **Validierung** über DTD oder XML Schema (XSD) [3][4]; **Namespaces** vermeiden Namenskonflikte [5].
- **Alle Werte sind Text** – Datentypen ergeben sich erst über ein Schema.
- Optionale **XML-Deklaration** `<?xml version="1.0" encoding="UTF-8"?>`, Unicode-Unterstützung,
  Kommentare, CDATA-Abschnitte.
- **Großes Ökosystem:** XPath, XSLT, XQuery, SOAP – verbreitet in Behörden, Banken, Industrie.
- Nachteil: deutlich mehr Overhead (Start- und End-Tag) als JSON.
- Medientyp: `application/xml` (bzw. `text/xml`) [6].

## 3. Wie unterscheidet die Applikation, ob JSON oder XML zurückgegeben wird?

Über **Content Negotiation mit dem HTTP-Header `Accept`** (proaktive Aushandlung, RFC 9110 Kap. 12.5.1 [7]):

1. Der Client schickt `Accept: application/json` oder `Accept: application/xml`.
2. Alle Controller deklarieren
   `produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}`
   (siehe `StationController`, `MeasurementController`, `AlertController`).
3. Spring MVC wertet den Header mit dem `ContentNegotiationManager` aus
   (`HeaderContentNegotiationStrategy`) [8]. In `WebConfig.configureContentNegotiation` ist eingestellt:
   nur der Accept-Header zählt (kein `?format=`-Parameter), Standard ist **JSON**
   (`defaultContentType(APPLICATION_JSON, ALL)`) – ohne Accept-Header oder mit `*/*` kommt also JSON.
4. Je nach Ergebnis serialisiert ein anderer `HttpMessageConverter` dasselbe Java-Objekt:
   `MappingJackson2HttpMessageConverter` (JSON) bzw. `MappingJackson2XmlHttpMessageConverter` (XML,
   aktiv weil `jackson-dataformat-xml` im Klassenpfad liegt) [9]. Die XML-Elementnamen kommen aus
   Annotationen wie `@JacksonXmlRootElement(localName = "measurement")`.
5. Wird ein anderes Format verlangt (z. B. `Accept: text/csv`), wirft Spring eine
   `HttpMediaTypeNotAcceptableException`, die im `GlobalExceptionHandler` zu **406 Not Acceptable**
   wird.
6. Auch Fehlerantworten folgen dem Accept-Header (`GlobalExceptionHandler.errorMediaType`).

Der Server teilt das gewählte Format im Antwort-Header `Content-Type` mit; der Webclient wertet diesen
Header aus und parst entsprechend mit `JSON.parse` oder `$.parseXML`.

## 4. Was zeichnet ein REST-Interface aus und warum wird es so oft verwendet?

REST (*Representational State Transfer*) ist ein Architekturstil, den Roy Fielding 2000 in seiner
Dissertation beschrieben hat [10, Kap. 5]. Er definiert folgende **Constraints**:

| Constraint | Bedeutung | In diesem Projekt |
|---|---|---|
| Client-Server | Trennung von Oberfläche und Datenhaltung | Webclient ↔ Spring-Boot-Server |
| Stateless | jede Anfrage enthält alle nötigen Infos, keine Session am Server | API-Key pro Request, keine Session |
| Cacheable | Antworten sind als cachebar/nicht cachebar gekennzeichnet | `Cache-Control: no-store` für Live-Daten |
| Uniform Interface | Ressourcen mit URIs, Manipulation über Repräsentationen, selbstbeschreibende Nachrichten, (HATEOAS) | `/api/v1/stations/{id}`, JSON/XML, Statuscodes |
| Layered System | Proxies/Gateways/Load-Balancer dazwischen möglich | – |
| Code on Demand (optional) | Server liefert ausführbaren Code | Webclient-JavaScript |

**Warum so verbreitet?**
- Baut auf **HTTP** auf – überall verfügbar, firewallfreundlich, vorhandene Infrastruktur (Proxies,
  Caches, Load-Balancer) funktioniert sofort.
- **Sprach- und plattformunabhängig**, einfache Formate (JSON/XML), mit jedem HTTP-Client testbar (curl, Browser).
- **Skalierbar** durch Zustandslosigkeit – jeder Server-Knoten kann jede Anfrage beantworten.
- **Lose Kopplung** zwischen Client und Server, einheitliche Semantik der HTTP-Methoden und Statuscodes.
- Gute **Werkzeugunterstützung** (OpenAPI/Swagger, Code-Generatoren).

Anmerkung: Nach dem Richardson Maturity Model [11] erreicht diese API Stufe 2 (Ressourcen + HTTP-Verben
+ Statuscodes); Hypermedia-Links (HATEOAS, Stufe 3) werden nicht verwendet.

## 5. Unterschied zwischen GET, POST, PUT und PATCH

Grundlage: RFC 9110 Kap. 9 [7] und RFC 5789 (PATCH) [12].

| Methode | Zweck | sicher (safe) | idempotent | typische Antwort | Beispiel hier |
|---|---|---|---|---|---|
| **GET** | Repräsentation einer Ressource lesen | ja | ja | 200 OK | `GET /api/v1/stations/ST-001` |
| **POST** | Daten zur Verarbeitung senden, meist neue Ressource anlegen (Server vergibt URI) | nein | **nein** | 201 Created + `Location` | `POST /api/v1/stations` |
| **PUT** | Ressource an bekannter URI **vollständig** ersetzen bzw. anlegen | nein | ja | 200 / 204 / 201 | (nicht umgesetzt) |
| **PATCH** | Ressource **teilweise** ändern | nein | nicht garantiert | 200 / 204 | `PATCH /api/v1/stations/ST-001` mit `{"isActive":false}` |

- *Sicher* (RFC 9110 Kap. 9.2.1): die Methode verändert den Serverzustand nicht → darf von Crawlern,
  Prefetching und Caches beliebig ausgeführt werden.
- *Idempotent* (Kap. 9.2.2): mehrfaches Ausführen hat denselben Effekt wie einmaliges → darf nach
  Netzwerkfehlern automatisch wiederholt werden. Zweimal `POST` legt zwei Ressourcen an (bzw. hier: 409),
  zweimal `PUT` mit demselben Body ergibt denselben Zustand.
- PUT schickt immer die **ganze** Ressource; fehlende Felder würden gelöscht. PATCH schickt nur die
  Änderungen [12, Kap. 2].

## 6. Neue Request-Methode / neuen Pfad hinzufügen – welche Stellen ändern?

Beispiel: `GET /api/v1/rivers` (Liste aller Flüsse mit Anzahl Stationen).

1. **Model/DTO** (`dto/`): neue Antwortklasse, z. B. `RiverList`, mit Jackson-XML-Annotationen
   (`@JacksonXmlRootElement`, `@JacksonXmlElementWrapper(useWrapping = false)`), damit JSON **und**
   XML sauber erzeugt werden.
2. **Repository** (`repository/`): nur falls neue Daten bzw. Abfragen nötig sind.
3. **Service** (`service/`): Geschäftslogik, Validierung der Parameter (`QueryParameterValidator`).
4. **Controller** (`controller/`): neue Methode mit `@GetMapping("/rivers")` (bzw. `@PostMapping`,
   `@PatchMapping` …) in bestehendem oder neuem `@RestController` unter `/api/v1`, mit
   `produces = {JSON, XML}`. Der Controller ruft nur den Service auf.
5. **Fehlerbehandlung** (`exception/`): neue Fehlerfälle als `ApiException`-Unterklasse; der
   `GlobalExceptionHandler` liefert sie automatisch im einheitlichen Format.
6. **Sicherheit** (`security/ApiKeyInterceptor`): schreibende Methoden sind automatisch geschützt;
   Regeln anpassen, falls nötig.
7. **OpenAPI-Doku**: `@Operation`, `@Parameter`, `@ApiResponse` ergänzen und `docs/openapi.yaml` neu
   exportieren.
8. **Tests** (`src/test/...`): MockMvc-Tests für JSON, XML und Fehlerfälle.
9. **Webclient** (`static/js/app.js`): neuer Aufruf über `apiGet()` und – wichtig – eine
   XML-Umwandlung für das neue Wurzelelement in `xml.document()`.
10. **README / Cheatsheet** aktualisieren.

## 7. Kann man OpenAPI und Swagger vergleichen?

**Nur eingeschränkt – sie sind keine Alternativen, sondern Standard und Werkzeug.**
Die *Swagger Specification* wurde 2015 von SmartBear an die **OpenAPI Initiative** (Linux Foundation)
übergeben und in **OpenAPI Specification** umbenannt (OpenAPI 2.0 = Swagger 2.0) [13][14].
Seitdem bezeichnet „OpenAPI" die Spezifikation und „Swagger" die Werkzeuge von SmartBear, die diese
Spezifikation umsetzen [13].

| | OpenAPI | Swagger |
|---|---|---|
| Was? | **Spezifikation** (Beschreibungsformat für HTTP-APIs in JSON/YAML) [15] | **Werkzeugsammlung**: Swagger UI, Swagger Editor, Swagger Codegen, SwaggerHub |
| Verantwortlich | OpenAPI Initiative / Linux Foundation, herstellerneutral | SmartBear Software |
| Zweck | Endpunkte, Parameter, Bodies, Statuscodes, Security einheitlich beschreiben | Spezifikation anzeigen, testen, bearbeiten, Code generieren |
| Versionen | 2.0, 3.0.x, 3.1.x, … | eigene Produktversionen |
| In diesem Projekt | springdoc erzeugt ein OpenAPI-3-Dokument: `/v3/api-docs`, `docs/openapi.yaml` | Swagger UI unter `/swagger-ui.html` stellt dieses Dokument interaktiv dar |

Vergleichbar ist also nur die Beziehung: *OpenAPI ist der „Bauplan"-Standard, Swagger eine (sehr
verbreitete) Implementierung von Werkzeugen dafür*. Andere Werkzeuge (Redoc, Postman, Stoplight)
verwenden ebenfalls OpenAPI.

## 8. Wo liegt der Fehler im Code-Snippet?

```java
@GetMapping("/stations/add")
public String createStation(@RequestParam String name, @RequestParam double warning) {
    stationService.save(new Station(name, warning));
    return "Station successfully created!";
}
```

**Hauptfehler: Eine zustandsändernde Operation (Anlegen) wird mit `GET` umgesetzt.**
GET ist laut RFC 9110 eine *sichere* Methode und darf den Serverzustand nicht verändern [7, Kap. 9.2.1].
Folgen: Browser-Prefetching, Suchmaschinen-Crawler, Link-Vorschauen oder automatische Wiederholungen
können ungewollt Stationen anlegen; Caches/Proxies dürfen GET-Antworten zwischenspeichern; ein simpler
Link oder `<img src="…/stations/add?name=x&warning=1">` reicht für einen CSRF-Angriff.

Weitere Fehler:
- **Verb in der URI** (`/add`): REST-URIs bezeichnen Ressourcen (Nomen); die Aktion ergibt sich aus der
  HTTP-Methode → `POST /api/v1/stations` [10, Kap. 5.2].
- **Daten als Query-Parameter statt Request-Body:** landen in Server-Logs, Browser-Verlauf und
  Proxy-Logs, Längenbeschränkung, keine strukturierten JSON/XML-Daten.
- **Falsche Antwort:** `200 OK` mit einem Text statt **`201 Created`** mit `Location`-Header und der
  angelegten Ressource als JSON/XML [7, Kap. 15.3.2].
- **Keine Validierung** (leerer Name, negative/unsinnige Grenzwerte, Warnwert < kritischer Wert),
  keine ID, keine Konfliktbehandlung (409), keine Authentifizierung für schreibende Zugriffe.
- Die Station ist unvollständig (Fluss, Position, Normal-/Kritischer Wert fehlen).

**Korrigiert** (so in `StationController` umgesetzt):

```java
@PostMapping(path = "/api/v1/stations",
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public ResponseEntity<Station> createStation(@Valid @RequestBody StationCreateRequest request) {
    Station created = stationService.createStation(request);   // 400 / 409 im Service
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(created.getId()).toUri();
    return ResponseEntity.created(location).body(created);     // 201 Created
}
```

---

## Quellen

1. T. Bray (Ed.): *RFC 8259 – The JavaScript Object Notation (JSON) Data Interchange Format*, IETF, 2017. https://www.rfc-editor.org/rfc/rfc8259
2. Ecma International: *ECMA-404 – The JSON Data Interchange Syntax*, 2. Ausgabe, 2017. https://ecma-international.org/publications-and-standards/standards/ecma-404/
3. W3C: *Extensible Markup Language (XML) 1.0 (Fifth Edition)*, 2008. https://www.w3.org/TR/xml/
4. W3C: *XML Schema Part 1: Structures*. https://www.w3.org/TR/xmlschema-1/
5. W3C: *Namespaces in XML 1.0 (Third Edition)*. https://www.w3.org/TR/xml-names/
6. IETF: *RFC 7303 – XML Media Types*, 2014. https://www.rfc-editor.org/rfc/rfc7303
7. R. Fielding, M. Nottingham, J. Reschke (Ed.): *RFC 9110 – HTTP Semantics*, IETF, 2022. https://www.rfc-editor.org/rfc/rfc9110
8. Spring Framework Reference: *Content Types / Content Negotiation (Web MVC)*. https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/content-negotiation.html
9. Spring Boot Reference: *Spring MVC – HttpMessageConverters*. https://docs.spring.io/spring-boot/reference/web/servlet.html
10. R. T. Fielding: *Architectural Styles and the Design of Network-based Software Architectures*, Dissertation, UC Irvine, 2000, Kapitel 5. https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm
11. M. Fowler: *Richardson Maturity Model*, 2010. https://martinfowler.com/articles/richardsonMaturityModel.html
12. L. Dusseault, J. Snell: *RFC 5789 – PATCH Method for HTTP*, IETF, 2010. https://www.rfc-editor.org/rfc/rfc5789
13. SmartBear: *What Is the Difference Between Swagger and OpenAPI?* https://swagger.io/blog/api-strategy/difference-between-swagger-and-openapi/
14. OpenAPI Initiative: *FAQ*. https://www.openapis.org/faq
15. OpenAPI Initiative: *OpenAPI Specification (latest)*. https://spec.openapis.org/oas/latest.html
