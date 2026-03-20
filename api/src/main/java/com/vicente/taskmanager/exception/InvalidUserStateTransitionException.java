package com.vicente.taskmanager.exception;

public class InvalidUserStateTransitionException extends RuntimeException {
    public InvalidUserStateTransitionException(String message) {
        super(message);
    }
}
