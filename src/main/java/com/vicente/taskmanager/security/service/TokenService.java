package com.vicente.taskmanager.security.service;

import com.vicente.taskmanager.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class TokenService {
    private final String issuer;
    private final SecretKey key;

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    public TokenService(@Value("${jwt_token_secret}") String secret, @Value("${spring.application.name}") String issuer) {
        this.issuer = issuer;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .issuer(this.issuer)
                .claim("userId", user.getId())
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(Date.from(this.expirationDate()))
                .signWith(this.key)
                .compact();
    }

    public String validateToken(String token) {
        try {
            JwtParser jwtParser = Jwts.parser().requireIssuer(this.issuer).verifyWith(this.key).build();
            Claims payload = jwtParser.parseSignedClaims(token).getPayload();
            return payload.getSubject();
        }catch (Exception e){
            logger.error("JWT validation error: {}", e.getMessage());
            return null;
        }
    }

    private Instant expirationDate() {
        return Instant.now().plus(2, ChronoUnit.MINUTES);
    }
}
