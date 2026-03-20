package com.vicente.taskmanager.domain.enums;

public enum TokenType {
    EMAIL_VERIFICATION(24L),
    PASSWORD_RESET(1L);

    private final long expirationHours;

    TokenType(long expirationHours) {
        this.expirationHours = expirationHours;
    }

    public long getExpirationHours() {
        return expirationHours;
    }
}
