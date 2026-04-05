package com.vicente.storage.exception;

public class StorageException extends RuntimeException {
    private final Integer statusCode;

    public StorageException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public StorageException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
