package com.example.floodmonitor.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Swagger UI: http://localhost:8080/swagger-ui.html
@Configuration
@OpenAPIDefinition(info = @Info(title = "Flood Monitor REST API", version = "1.0",
        description = "Hochwasser-Messstationen. Format ueber Accept-Header: application/json (Standard) "
                + "oder application/xml, sonst 406. GET ist offen, POST/DELETE brauchen den Header X-API-Key "
                + "(fehlt/falsch -> 401, Lese-Key -> 403). Fehler kommen immer als ErrorResponse."))
@SecurityScheme(name = "apiKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "X-API-Key")
public class OpenApiConfig {
}
