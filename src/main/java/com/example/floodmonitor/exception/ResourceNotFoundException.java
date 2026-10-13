package com.example.floodmonitor.exception;

import org.springframework.http.HttpStatus;

/** 404 Not Found */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public static ResourceNotFoundException station(String stationId) {
        return new ResourceNotFoundException("Station " + stationId + " wurde nicht gefunden");
    }
}
