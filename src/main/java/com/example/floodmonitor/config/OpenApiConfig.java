package com.example.floodmonitor.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI-Metadaten. Dokumentation: /swagger-ui.html, Spezifikation: /v3/api-docs bzw. /v3/api-docs.yaml
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Flood Monitor REST API",
                version = "1.0.0",
                description = """
                        REST-Schnittstelle eines Hochwasser-Fruehwarnsystems.

                        **Formate:** Alle Ressourcen sind als JSON (Standard) und XML verfuegbar.
                        Das Format wird ueber den `Accept`-Header gewaehlt (`application/json` oder `application/xml`).
                        Ohne Accept-Header wird JSON geliefert, nicht unterstuetzte Formate liefern **406 Not Acceptable**.

                        **Authentifizierung:** Lesende Zugriffe (GET) sind oeffentlich. Schreibende Zugriffe
                        (POST, PATCH, DELETE) benoetigen den Header `X-API-Key`. Fehlender/ungueltiger Schluessel -> 401,
                        Lese-Schluessel ohne Schreibrecht -> 403.

                        **Fehler:** Alle Fehler werden einheitlich als `ErrorResponse` (JSON oder XML) geliefert.
                        """),
        servers = @Server(url = "http://localhost:8080", description = "Lokale Entwicklung"))
@SecurityScheme(name = "apiKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key", description = "API-Schluessel fuer schreibende Zugriffe")
public class OpenApiConfig {
}
