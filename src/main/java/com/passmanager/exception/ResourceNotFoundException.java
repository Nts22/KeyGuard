package com.passmanager.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s no encontrado con id: %d", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
