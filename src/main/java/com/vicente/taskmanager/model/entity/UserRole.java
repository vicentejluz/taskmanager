package com.vicente.taskmanager.model.entity;

public enum UserRole {
    USER("User"),
    ADMIN("Admin");

    private final String value;

    UserRole(String role) {
        this.value = role;
    }

    public String getValue() {
        return value;
    }
}
