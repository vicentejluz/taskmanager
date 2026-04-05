package com.vicente.storage.exception;

public class SignedUrlValidationException extends RuntimeException {
    private final int statusCode;

    public SignedUrlValidationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public SignedUrlValidationException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
