package com.vicente.taskmanager.exception;

public class TaskStatusNotAllowedException extends RuntimeException {
    public TaskStatusNotAllowedException(String message) {
        super(message);
    }
}
