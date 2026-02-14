package com.vicente.taskmanager.security.entrypoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;


@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        String message = "Invalid or expired JWT token.";

        response.getWriter().write("""
        {
          "timestamp": "%s",
          "status": %d,
          "error": "Unauthorized",
          "message": "%s",
          "path": "%s"
        }
        """.formatted(
                Instant.now(),
                response.getStatus(),
                message,
                request.getRequestURI()
        ));
    }
}
