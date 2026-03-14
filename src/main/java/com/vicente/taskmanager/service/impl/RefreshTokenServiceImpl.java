package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.exception.RefreshTokenException;
import com.vicente.taskmanager.repository.RefreshTokenRepository;
import com.vicente.taskmanager.service.EmailService;
import com.vicente.taskmanager.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpiration;
    private final long refreshTokenGraceWindow;
    private final EmailService emailService;
    // SecureRandom é um gerador de números aleatórios
    // criptograficamente seguro (CSPRNG).
    // Diferente de Random, ele NÃO é previsível.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.refresh.token.expiration.days}") long refreshExpiration,
            @Value("${security.refresh.token.grace.window.seconds}") long refreshTokenGraceWindow,
            EmailService emailService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiration = refreshExpiration;
        this.refreshTokenGraceWindow = refreshTokenGraceWindow;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public String create(User user, String oldRefreshToken) {
        logger.info("Creating refresh token (login flow) | userId={}", user.getId());

        revokeOldRefreshTokenIfOwnedByUser(user.getId(), oldRefreshToken);

        UUID tokenFamilyId = UUID.randomUUID();

        return createNewToken(user, tokenFamilyId);
    }

    @Override
    @Transactional
    public String create(User user, RefreshToken oldRefreshToken) {
        logger.info("Creating refresh token (rotation flow) | userId={}", user.getId());

        if (oldRefreshToken != null) {
            logger.debug("Revoking previous refresh token | tokenId={} | userId={}",
                    oldRefreshToken.getId(), user.getId());
            oldRefreshToken.setRevokedAt(OffsetDateTime.now());
        }

        return createNewToken(user, Objects.requireNonNull(oldRefreshToken).getTokenFamilyId());
    }

    @Override
    public void validate(RefreshToken refreshToken) {
        logger.info("Validating refresh token | tokenPrefix={}", tokenPrefix(refreshToken.getToken()));

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            logger.debug("Expired refresh token | tokenPrefix={}", tokenPrefix(refreshToken.getToken()));
            throw new RefreshTokenException("Refresh token is expired!");
        }

        logger.info("Refresh token validated | tokenPrefix={}", tokenPrefix(refreshToken.getToken()));
    }

    @Override
    @Transactional
    public void revokeToken(String token, Long userId) {
        logger.info("Revoking Refresh token | tokenPrefix={}", tokenPrefix(token));
        RefreshToken refreshToken = findByToken(token);

        if (!refreshToken.getUser().getId().equals(userId)) {
            logger.debug("Refresh token does not belong to the authenticated user | tokenUserId={} | requestUserId={}",
                    refreshToken.getUser().getId(), userId);
            throw new RefreshTokenException("Refresh token does not belong to the authenticated user.");
        }

        if (refreshToken.getRevokedAt() != null)
            return;

        refreshToken.setRevokedAt(OffsetDateTime.now());

        refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token revoked successfully. | tokenPrefix={}", tokenPrefix(token));
    }

    @Override
    @Transactional
    public void revokeAllTokens(Long userId) {
        logger.info("Revoking all refresh tokens | userId={}", userId);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUser_IdAndRevokedAtNull(userId);

        if (refreshTokens.isEmpty())
            return;

        refreshTokens.forEach(refreshToken -> refreshToken.setRevokedAt(OffsetDateTime.now()));

        refreshTokenRepository.saveAll(refreshTokens);
        logger.info("All refresh tokens revoked successfully | userId={} | count={}", userId, refreshTokens.size());
    }

    @Override
    @Transactional
    public void revokeAllTokensExceptCurrentToken(Long userId, String currentRefreshToken) {
        logger.info("Revoking all refresh tokens current refresh token | userId={}", userId);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUser_IdAndRevokedAtNull(userId);

        if (refreshTokens.isEmpty())
            return;

        refreshTokens.forEach(refreshToken -> {
            if (!refreshToken.getToken().equals(currentRefreshToken)) {
                refreshToken.setRevokedAt(OffsetDateTime.now());
            }
        });

        refreshTokenRepository.saveAll(refreshTokens);
        logger.info("All refresh tokens revoked except current token successfully | userId={} | count={}",
                userId, refreshTokens.size());
    }

    @Override
    @Transactional
    public void handleReuseAttack(RefreshToken oldRefreshToken, String ipAddress) {
        if (oldRefreshToken.isReuseDetected()) {
            logger.debug("Reuse already handled | tokenId={}", oldRefreshToken.getId());
            return;
        }

        if(oldRefreshToken.getRevokedAt().isBefore(OffsetDateTime.now().minusSeconds(refreshTokenGraceWindow))) {
            logger.warn("Refresh token reuse detected | id={}", oldRefreshToken.getId());

            User user = oldRefreshToken.getUser();

            revokeFamilyTokens(user.getId(), oldRefreshToken.getTokenFamilyId());

            user.incrementTokenVersion();

            refreshTokenRepository.markReuseDetected(oldRefreshToken.getId());

            emailService.sendSecurityAlert(user.getEmail(), ipAddress);
        }
    }

    @Override
    @Transactional
    public RefreshToken findByTokenForUpdate(String token) {
        return refreshTokenRepository.findByTokenForUpdate(token).orElseThrow(() -> {
            logger.debug("Invalid refresh token");
            return new RefreshTokenException("Refresh token invalid!");
        });
    }

    private RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(hashToken(token)).orElseThrow(() -> {
            logger.debug("Invalid refresh token | tokenPrefix={}", tokenPrefix(token));
            return new RefreshTokenException("Refresh token invalid!");
        });
    }

    private String tokenPrefix(String token) {
        return (token != null) ? token.substring(0, 8) : null;
    }

    private String createNewToken(User user, UUID tokenFamilyId) {
        String token = generateToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(refreshExpiration);
        RefreshToken refreshToken = new RefreshToken(hashToken(token), tokenFamilyId, expiresAt, user);

        refreshTokenRepository.saveAndFlush(refreshToken);
        logger.info("Refresh token created successfully | tokenId={} | userId={} | expiresAt={}",
                refreshToken.getId(), user.getId(), expiresAt);
        return token;
    }

    private void revokeOldRefreshTokenIfOwnedByUser(long userId, String oldRefreshToken) {
        if (oldRefreshToken == null)
            return;

        Optional<RefreshToken> optionalOldRefreshToken = refreshTokenRepository.findByToken(hashToken(oldRefreshToken));

        optionalOldRefreshToken.ifPresent(refreshToken -> {
            if (refreshToken.getUser().getId().equals(userId)) {
                refreshToken.setRevokedAt(OffsetDateTime.now());
                refreshTokenRepository.saveAndFlush(refreshToken);
                logger.debug("Previous refresh token revoked | tokenId={} | userId={}",
                        refreshToken.getId(), userId);
            }
        });
    }

    private void revokeFamilyTokens(Long userId, UUID tokenFamilyId) {
        logger.info("Revoking family tokens | userId={} tokenFamilyId={}", userId, tokenFamilyId);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUser_IdAndTokenFamilyId(userId, tokenFamilyId);

        if (refreshTokens.isEmpty())
            return;

        OffsetDateTime now = OffsetDateTime.now();

        refreshTokens.forEach(refreshToken -> refreshToken.setRevokedAt(now));

        refreshTokenRepository.saveAll(refreshTokens);
        logger.info("All family tokens revoked successfully | userId={} tokenFamilyId={} count={}",
                userId, refreshTokens.size(), tokenFamilyId);
    }


    /**
     * Gera um Refresh Token seguro usando criptografia forte.

     * Estratégia:
     * - Usa SecureRandom (CSPRNG do sistema operacional)
     * - Gera 32 bytes (256 bits de entropia)
     * - Converte para Base64 URL Safe

     * Esse token será enviado ao cliente (cookie HttpOnly).
     */
    private String generateToken(){
        // Cria um array de 32 bytes.
        // 32 bytes = 256 bits.
        // Isso fornece altíssimo nível de segurança contra ataques de força bruta.
        byte[] bytes = new byte[32];

        // Preenche o array com bytes aleatórios seguros.
        // Esses valores são gerados usando fontes seguras do sistema operacional.
        SECURE_RANDOM.nextBytes(bytes);

        return getString(bytes);
    }

    /**
     * Gera o hash SHA-256 do Refresh Token.

     * IMPORTANTE:
     * - O banco de dados NÃO deve armazenar o token puro.
     * - Apenas o hash deve ser armazenado.
     * - Isso protege contra vazamento do banco.

     * Processo:
     * - Recebe o token
     * - Converte para bytes UTF-8
     * - Aplica SHA-256
     * - Converte o resultado para Base64
     */
    public static String hashToken(String token){
        // MessageDigest é a classe padrão do Java para calcular hashes.
        MessageDigest messageDigest;
        try {
            // SHA-256 é um algoritmo criptográfico seguro e irreversível.
            messageDigest = MessageDigest.getInstance("SHA-256");

        } catch (NoSuchAlgorithmException e) {
            logger.error("Hash Algorithm Not Supported!", e);
            throw new IllegalStateException("SHA-256 not available", e);
        }

        // Converte o token (String) para bytes usando UTF-8.
        // Hash functions trabalham com bytes, não com texto.
        // Calcula o hash SHA-256.
        // O resultado será sempre:
        // - 32 bytes (256 bits)
        // Independentemente do tamanho do token original.
        byte[] hash = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));

        return getString(hash);
    }

    private static String getString(byte[] bytes) {
        // Converte os bytes em String usando Base64 URL Safe.
        // Isso é necessário porque:
        // - Bytes não podem ser enviados diretamente em HTTP
        // - Base64 transforma bytes em texto
        // - UrlEncoder evita caracteres problemáticos (+, /)
        //
        // withoutPadding() remove o caractere '=' no final.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
