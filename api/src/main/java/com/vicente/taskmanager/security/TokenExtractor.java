package com.vicente.taskmanager.security;

import jakarta.servlet.http.HttpServletRequest;

public final class TokenExtractor {
    private static final String BEARER_PREFIX = "Bearer ";

    private TokenExtractor() {
    }

    public static String extractAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
