package com.vicente.storage.exception;

public class StorageException extends RuntimeException {
    private final Integer statusCode;

    public StorageException(String message, int codeStatus) {
        super(message);
        this.statusCode = codeStatus;
    }

    public StorageException(String message, int codeStatus, Throwable cause) {
        super(message, cause);
        this.statusCode = codeStatus;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
