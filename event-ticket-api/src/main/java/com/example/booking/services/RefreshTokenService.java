package com.example.booking.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.example.booking.domain.entities.RefreshToken;
import com.example.booking.domain.entities.User;
import com.example.booking.exception.TokenRefreshException;
import com.example.booking.repositories.RefreshTokenRepository;
import com.example.booking.services.intefaces.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    @Value("${booking.app.jwtRefreshExpirationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserService userService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserService userService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
    }

    public Optional<RefreshToken> findByToken(String token) {
        log.debug("Searching for refresh token in database");
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(UUID userId) {
        log.info("Creating refresh token for userId={}", userId);

        Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByUser_UserId(userId);

        if (existingTokenOpt.isPresent()) {
            RefreshToken existingToken = existingTokenOpt.get();
            if (existingToken.getExpiryDate().isAfter(Instant.now())) {
                log.debug("Returning valid existing refresh token for userId={}", userId);
                return existingToken;
            } else {
                log.info("Existing refresh token expired. Deleting it and generating a new one for userId={}", userId);
                refreshTokenRepository.delete(existingToken);
            }
        }

        User user = userService.findUserEntityById(userId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("New refresh token created for userId={}", userId);
        return saved;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        log.debug("Verifying expiration of refresh token");

        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            log.warn("Refresh token expired. Token={} Expiry={}", token.getToken(), token.getExpiryDate());
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }

        log.debug("Refresh token is still valid. Expiry={}", token.getExpiryDate());
        return token;
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        log.info("Deleting refresh token(s) for userId={}", userId);
        refreshTokenRepository.deleteByUser(userService.findUserEntityById(userId));
    }
}
