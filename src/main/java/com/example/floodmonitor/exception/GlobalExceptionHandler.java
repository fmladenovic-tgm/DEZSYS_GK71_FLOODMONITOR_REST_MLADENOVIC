package com.example.floodmonitor.exception;

import com.example.floodmonitor.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.util.MimeTypeUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Zentrale Fehlerbehandlung. Alle Fehler werden im einheitlichen {@link ErrorResponse}-Format
 * ausgegeben - als XML, wenn der Client XML bevorzugt, sonst als JSON. Stacktraces werden nie ausgeliefert.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final List<MediaType> SUPPORTED = List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(ex.getStatus());
        if (ex instanceof UnauthorizedException) {
            builder.header(HttpHeaders.WWW_AUTHENTICATE, "ApiKey header=\"X-API-Key\"");
        }
        return build(builder, ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String expected = ex.getRequiredType() == null ? "?" : ex.getRequiredType().getSimpleName();
        String message = "Parameter '" + ex.getName() + "' hat einen ungueltigen Wert '" + ex.getValue()
                + "' (erwartet: " + expected + ")";
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Pflichtparameter '" + ex.getParameterName() + "' fehlt", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Ungueltiger Request-Body: " + details, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex,
                                                                HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Ungueltige Eingabe", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST,
                "Request-Body fehlt, ist syntaktisch falsch oder enthaelt unbekannte bzw. unzulaessige Felder",
                request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                             HttpServletRequest request) {
        // Antwort immer als JSON, da das angefragte Format nicht erzeugt werden kann
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        ErrorResponse body = body(HttpStatus.NOT_ACCEPTABLE,
                "Format '" + accept + "' wird nicht unterstuetzt. Unterstuetzt: application/json, application/xml",
                request);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                                                    HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Medientyp '" + ex.getContentType() + "' wird nicht unterstuetzt. Unterstuetzt: application/json, application/xml",
                request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP-Methode " + ex.getMethod() + " ist fuer diese Ressource nicht erlaubt", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Ressource " + request.getRequestURI() + " existiert nicht", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Details nur ins Server-Log, niemals an den Client
        log.error("Unerwarteter Fehler bei {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Interner Serverfehler", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        return build(ResponseEntity.status(status), status, message, request);
    }

    private ResponseEntity<ErrorResponse> build(ResponseEntity.BodyBuilder builder, HttpStatus status, String message,
                                                HttpServletRequest request) {
        return builder.contentType(errorMediaType(request)).body(body(status, message, request));
    }

    private ErrorResponse body(HttpStatus status, String message, HttpServletRequest request) {
        return new ErrorResponse(clock.instant().truncatedTo(ChronoUnit.SECONDS), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI());
    }

    /**
     * Waehlt das Fehlerformat anhand des Accept-Headers: XML nur wenn explizit bevorzugt, sonst JSON.
     * Das Format wird fest gesetzt, damit auch bei exotischen Accept-Headern eine Fehlermeldung ankommt.
     */
    static MediaType errorMediaType(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept == null || accept.isBlank()) {
            return MediaType.APPLICATION_JSON;
        }
        try {
            List<MediaType> requested = MediaType.parseMediaTypes(accept);
            MimeTypeUtils.sortBySpecificity(requested);
            for (MediaType type : requested) {
                if (type.isWildcardType()) {
                    return MediaType.APPLICATION_JSON;
                }
                for (MediaType supported : SUPPORTED) {
                    if (supported.isCompatibleWith(type)) {
                        return supported;
                    }
                }
                if (type.isCompatibleWith(MediaType.TEXT_XML)) {
                    return MediaType.APPLICATION_XML;
                }
            }
        } catch (RuntimeException ignored) {
            // ungueltiger Accept-Header -> JSON
        }
        return MediaType.APPLICATION_JSON;
    }
}
