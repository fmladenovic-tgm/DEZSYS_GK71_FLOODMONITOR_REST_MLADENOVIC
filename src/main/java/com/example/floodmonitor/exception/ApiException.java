package com.example.floodmonitor.exception;

import org.springframework.http.HttpStatus;

// eigene Exception mit HTTP-Status, z.B. new ApiException(HttpStatus.NOT_FOUND, "...")
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
