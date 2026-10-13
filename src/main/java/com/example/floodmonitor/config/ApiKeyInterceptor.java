package com.example.floodmonitor.config;

import com.example.floodmonitor.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Lesen (GET) darf jeder, fuer POST/DELETE braucht man den Header X-API-Key
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Value("${security.api-key.admin}")
    private String adminKey;

    @Value("${security.api-key.reader}")
    private String readerKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getMethod().equals("GET")) {
            return true;
        }
        String key = request.getHeader("X-API-Key");
        if (key == null || key.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Header X-API-Key fehlt");
        }
        if (key.equals(adminKey)) {
            return true;
        }
        if (key.equals(readerKey)) {
            // gueltiger Key, aber nur zum Lesen
            throw new ApiException(HttpStatus.FORBIDDEN, "Keine Schreibberechtigung");
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Ungueltiger API-Key");
    }
}
