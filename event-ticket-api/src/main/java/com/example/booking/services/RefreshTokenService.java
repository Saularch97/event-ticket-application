package com.example.booking.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.example.booking.domain.entities.RefreshToken;
import com.example.booking.domain.entities.User;
import com.example.booking.exception.TokenRefreshException;
import com.example.booking.exception.UserNotFoundException;
import com.example.booking.repositories.RefreshTokenRepository;
import com.example.booking.repositories.UserRepository;
import com.example.booking.services.intefaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class RefreshTokenService {
    @Value("${booking.app.jwtRefreshExpirationMs}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserService userService;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(UUID userId) {

        Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByUser_UserId(userId);

        if (existingTokenOpt.isPresent()) {
            RefreshToken existingToken = existingTokenOpt.get();

            if (existingToken.getExpiryDate().isAfter(Instant.now())) {
                return existingToken;
            } else {
                refreshTokenRepository.delete(existingToken);
            }
        }

        User user = userService.findUserEntityById(userId);

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }

        return token;
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        refreshTokenRepository.deleteByUser(userService.findUserEntityById(userId));
    }
}
