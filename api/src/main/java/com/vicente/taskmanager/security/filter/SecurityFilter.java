package com.vicente.taskmanager.security.filter;

import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.security.util.TokenExtractor;
import com.vicente.taskmanager.cache.AuthTokenStoreService;
import com.vicente.taskmanager.security.service.TokenService;
import com.vicente.taskmanager.security.service.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final AuthTokenStoreService authTokenStoreService;
    private final UserDetailsServiceImpl userDetailsService;

    SecurityFilter(TokenService tokenService, AuthTokenStoreService authTokenStoreService, UserDetailsServiceImpl userDetailsService) {
        this.tokenService = tokenService;
        this.authTokenStoreService = authTokenStoreService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
            String token = TokenExtractor.extractAccessToken(request);
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = tokenService.getClaims(token);
                if (claims != null) {
                    String jti = claims.getId();
                    if(!authTokenStoreService.isBlacklisted(jti)){
                        String subject = claims.getSubject();
                        Long tokenVersion = claims.get("tokenVersion", Long.class);
                        User user = (User) userDetailsService.loadUserByUsername(subject);
                        if (user.getDeletedAt() == null &&
                            user.isAccountNonLocked() &&
                            user.isEnabled() && tokenVersion.equals(user.getTokenVersion())) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(user, null,
                                            user.getAuthorities());

                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                }
            }
        filterChain.doFilter(request, response);
    }
}
