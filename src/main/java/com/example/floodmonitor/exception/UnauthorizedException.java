package com.example.floodmonitor.exception;

import org.springframework.http.HttpStatus;

/** 401 Unauthorized - Anmeldung (API-Key) fehlt oder ist ungueltig. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
