package com.example.floodmonitor.exception;

import org.springframework.http.HttpStatus;

/** 403 Forbidden - angemeldet, aber keine Berechtigung. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
