package com.example.booking.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.booking.domain.entities.RefreshToken;
import com.example.booking.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;



@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    void deleteByUser(User user);

    Optional<RefreshToken> findByUser_UserId(UUID userId);
}
