package com.vicente.taskmanager.domain.enums;

public enum UserRole {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String value;

    UserRole(String role) {
        this.value = role;
    }

    public String getValue() {
        return value;
    }
}
