package com.example.floodmonitor.exception;

import org.springframework.http.HttpStatus;

/** 409 Conflict - z.B. Station mit dieser ID existiert bereits. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
