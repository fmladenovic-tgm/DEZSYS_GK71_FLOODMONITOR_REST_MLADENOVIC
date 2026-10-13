package com.example.floodmonitor.exception;

import org.springframework.http.HttpStatus;

/** 400 Bad Request - ungueltige Eingabe (Query-Parameter oder Request-Body). */
public class InvalidParameterException extends ApiException {

    public InvalidParameterException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
