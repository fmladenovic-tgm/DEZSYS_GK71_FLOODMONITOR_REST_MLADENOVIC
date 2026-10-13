package com.example.floodmonitor.security;

import com.example.floodmonitor.exception.ForbiddenException;
import com.example.floodmonitor.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/**
 * Einfache API-Key-Authentifizierung.
 * <ul>
 *   <li>Lesende Zugriffe (GET, HEAD, OPTIONS) sind oeffentlich.</li>
 *   <li>Schreibende Zugriffe (POST, PUT, PATCH, DELETE) benoetigen den Header {@code X-API-Key}.</li>
 *   <li>Fehlender oder unbekannter Schluessel -&gt; 401 Unauthorized.</li>
 *   <li>Gueltiger Lese-Schluessel ohne Schreibrecht -&gt; 403 Forbidden.</li>
 * </ul>
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-API-Key";
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final byte[] adminKey;
    private final byte[] readerKey;

    public ApiKeyInterceptor(@Value("${security.api-key.admin}") String adminKey,
                             @Value("${security.api-key.reader}") String readerKey) {
        this.adminKey = adminKey.getBytes(StandardCharsets.UTF_8);
        this.readerKey = readerKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (READ_METHODS.contains(request.getMethod())) {
            return true;
        }
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            throw new UnauthorizedException("Anmeldung erforderlich: Header " + HEADER + " fehlt");
        }
        byte[] given = key.getBytes(StandardCharsets.UTF_8);
        // zeitkonstanter Vergleich gegen Timing-Angriffe
        if (MessageDigest.isEqual(given, adminKey)) {
            return true;
        }
        if (MessageDigest.isEqual(given, readerKey)) {
            throw new ForbiddenException("Dieser API-Schluessel hat keine Schreibberechtigung");
        }
        throw new UnauthorizedException("Ungueltiger API-Schluessel");
    }
}
