package com.vicente.taskmanager.controller.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieHelper {
    private static final String COOKIE_PATH = "/api/v1";
    private CookieHelper() {}

    public static HttpHeaders createHeaders(ResponseCookie... cookies) {
        HttpHeaders headers = new HttpHeaders();

        for (ResponseCookie cookie : cookies) {
            headers.add(HttpHeaders.SET_COOKIE,  cookie.toString());
        }
        return headers;
    }

    public static ResponseCookie createCookie(String name, String value, Duration duration, boolean httpOnly) {
        return ResponseCookie.from(name, value)
                .path(COOKIE_PATH)
                .httpOnly(httpOnly)
                .sameSite("Strict")
                .maxAge(duration)
                .build();
    }
}
