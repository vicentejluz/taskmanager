package com.vicente.taskmanager.model.enums;

import com.vicente.taskmanager.exception.InvalidAccountStatusException;

import java.util.Objects;

public enum AccountStatus {
    PENDING_VERIFICATION("Pending"),
    DISABLED_BY_ADMIN("Disabled"),
    ACTIVE("Active");

    private final String value;

    AccountStatus(String status) {
        this.value = status;
    }

    public String getValue() {
        return value;
    }

    public static AccountStatus convert(String status) {
        if(Objects.nonNull(status) && !status.isBlank()) {
            for (AccountStatus accountStatus : values()) {
                if (accountStatus.getValue().equalsIgnoreCase(status) ||
                        accountStatus.name().equalsIgnoreCase(status)) {
                    return accountStatus;
                }
            }
            throw new InvalidAccountStatusException("Invalid Account status: " + status);
        }
        return null;
    }
}
