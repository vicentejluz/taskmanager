package com.vicente.taskmanager.exception;

public class UserOperationNotAllowedException extends RuntimeException {
    public UserOperationNotAllowedException(String message) {
        super(message);
    }
}
