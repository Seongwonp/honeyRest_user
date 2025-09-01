package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.RefreshToken;
import com.honeyrest.honeyrest_user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenValue(String tokenValue);
    void deleteByUser(User user);
}
