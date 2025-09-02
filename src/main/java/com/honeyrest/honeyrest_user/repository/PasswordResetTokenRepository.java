package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenValue(String tokenValue);
}
