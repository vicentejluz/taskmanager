package com.vicente.taskmanager.security.service;

import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.entity.UserRole;
import com.vicente.taskmanager.security.model.JWTUserData;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TokenService {
    @Value("${jwt_token_secret}")
    private String secret;

    @Value("${spring.application.name}")
    private String issuer;

    private SecretKey key;

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    @PostConstruct
    private void init(){
        key = Keys.hmacShaKeyFor(this.secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .issuer(this.issuer)
                .claim("userId", user.getId())
                .claim("roles", user.getRoles().stream().map(UserRole::name).toList())
                .subject(user.getEmail())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(this.expirationDate()))
                .signWith(key)
                .compact();
    }

    public Optional<JWTUserData> validateToken(String token) {
        try {
            JwtParser jwtParser = Jwts.parser().requireIssuer(this.issuer).verifyWith(key).build();
            Claims payload = jwtParser.parseSignedClaims(token).getPayload();

            Long id = payload.get("userId", Long.class);
            Collection<?> rawRoles = payload.get("roles", Collection.class);
            Set<UserRole> roles = rawRoles.stream().map(String.class::cast).map(UserRole::valueOf)
                    .collect(Collectors.toUnmodifiableSet());
            String subject = payload.getSubject();
            return Optional.of(new JWTUserData(id, subject, roles));
        }catch (Exception e){
            logger.error("JWT validation error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Instant expirationDate() {
        return Instant.now().plusSeconds(120);
    }
}
