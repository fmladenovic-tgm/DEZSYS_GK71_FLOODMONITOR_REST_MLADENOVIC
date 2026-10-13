package com.example.floodmonitor.exception;

import com.example.floodmonitor.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

// Zentrale Fehlerbehandlung -> alle Fehler im selben Format, nie mit Stacktrace
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        return error(ex.getStatus(), ex.getMessage(), req);
    }

    // z.B. limit=abc oder isActive=vielleicht
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Parameter '" + ex.getName() + "' hat einen ungueltigen Wert: " + ex.getValue(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadBody(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Request-Body ist ungueltig", req);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest req) {
        // das gewuenschte Format geht ja nicht -> Fehler immer als JSON
        ErrorResponse body = body(HttpStatus.NOT_ACCEPTABLE, "Format wird nicht unterstuetzt, nur application/json oder application/xml", req);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type " + ex.getContentType() + " wird nicht unterstuetzt", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Methode " + ex.getMethod() + " ist hier nicht erlaubt", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "Pfad existiert nicht", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleRest(Exception ex, HttpServletRequest req) {
        log.error("Unerwarteter Fehler", ex); // nur ins Log, nicht an den Client
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Interner Serverfehler", req);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest req) {
        // Fehler als XML wenn der Client XML will, sonst JSON
        String accept = req.getHeader("Accept");
        MediaType type = accept != null && accept.contains("xml") ? MediaType.APPLICATION_XML : MediaType.APPLICATION_JSON;
        return ResponseEntity.status(status).contentType(type).body(body(status, message, req));
    }

    private ErrorResponse body(HttpStatus status, String message, HttpServletRequest req) {
        return new ErrorResponse(clock.instant().truncatedTo(ChronoUnit.SECONDS), status.value(),
                status.getReasonPhrase(), message, req.getRequestURI());
    }
}
