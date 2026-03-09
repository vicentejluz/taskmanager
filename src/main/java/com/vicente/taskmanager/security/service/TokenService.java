package com.vicente.taskmanager.security.service;

import com.vicente.taskmanager.domain.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
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
    private final Long expiration;
    private final JwtParser jwtParser;

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    public TokenService(
            @Value("${jwt.token.secret}") String secret,
            @Value("${jwt.token.issuer}") String issuer,
            @Value("${jwt.token.expiration}")Long expiration
    ) {
        this.issuer = issuer;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.jwtParser = Jwts.parser().requireIssuer(this.issuer).verifyWith(this.key).build();
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .issuer(this.issuer)
                .id(UUID.randomUUID().toString())
                .claim("userId", user.getId())
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(Date.from(this.expirationDate()))
                .signWith(this.key)
                .compact();
    }

    public Claims getClaims(String token) {
        Throwable ex = null;
        try {
            return parseToken(token);
        }catch(SignatureException | MalformedJwtException | ExpiredJwtException | IllegalArgumentException e) {
            ex = e;
        } catch (Exception _){
        }
        loggerError(ex);
        return null;
    }

    private Claims parseToken(String token) {
        Jwts.parser().requireIssuer(this.issuer).verifyWith(this.key).build();
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    private void loggerError(Throwable ex) {

        if (ex != null) {
            switch (ex) {
                case SignatureException _ -> logger.error("Invalid JWT Signature");
                case MalformedJwtException _ -> logger.error("Invalid JWT Token");
                case ExpiredJwtException _ -> logger.error("Expired JWT Token");
                case IllegalArgumentException _ -> logger.error("JWT claims string is empty");
                default -> logger.error("JWT validation error: {}", ex.getMessage());
            }
        }
    }

    private Instant expirationDate() {
        return Instant.now().plus(expiration, ChronoUnit.MINUTES);
    }
}
