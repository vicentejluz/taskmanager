package com.vicente.taskmanager.model.entity;

import com.vicente.taskmanager.exception.InvalidTaskStatusException;

public enum TaskStatus {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    DONE("Done"),
    CANCELLED("Cancelled");

    private final String value;

    TaskStatus(String status) {
        this.value = status;
    }

    public String getValue() {
        return value;
    }

    public static TaskStatus converter(String status) {
        try {
            return TaskStatus.valueOf(status.toUpperCase().trim());
        }catch (IllegalArgumentException e) {
            throw new InvalidTaskStatusException("Invalid task status: " + status);
        }
    }
}
