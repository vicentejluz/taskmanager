package com.vicente.taskmanager.security.filter;

import com.vicente.taskmanager.model.entity.UserRole;
import com.vicente.taskmanager.security.model.JWTUserData;
import com.vicente.taskmanager.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    SecurityFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = retrieveToken(request);
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<JWTUserData> user = tokenService.validateToken(token);
                if (user.isPresent()) {
                    Set<SimpleGrantedAuthority> authorities = user.get().roles().stream().map(UserRole::getValue)
                            .map(SimpleGrantedAuthority::new).collect(Collectors.toUnmodifiableSet());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user.get(), null,
                                    authorities);

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication", e);
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    private String retrieveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length());
        }
        return null;
    }
}
