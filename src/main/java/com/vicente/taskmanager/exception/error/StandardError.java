package com.vicente.taskmanager.exception.error;

import java.time.Instant;

public record StandardError(Instant timestamp, Integer status, String error, String message, String path) {
}
