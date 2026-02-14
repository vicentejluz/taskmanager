package com.vicente.taskmanager.security.entrypoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       @NonNull AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        String message = "You do not have permission to access this resource.";

        response.getWriter().write("""
        {
          "timestamp": "%s",
          "status": %d,
          "error": "Forbidden",
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
