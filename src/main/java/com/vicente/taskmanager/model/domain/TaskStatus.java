package com.vicente.taskmanager.model.domain;

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

    public static TaskStatus converter(String status) throws IllegalArgumentException {
        return TaskStatus.valueOf(status.toUpperCase().trim());
    }
}
