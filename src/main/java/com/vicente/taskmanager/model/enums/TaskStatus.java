package com.vicente.taskmanager.model.enums;

import com.vicente.taskmanager.exception.InvalidTaskStatusException;

import java.util.Objects;

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

    public static TaskStatus convert(String status) {
        try {
            if(Objects.nonNull(status) && !status.isBlank())
                return TaskStatus.valueOf(status.toUpperCase().trim());
            return null;
        }catch (IllegalArgumentException e) {
            throw new InvalidTaskStatusException("Invalid task status: " + status);
        }
    }
}
