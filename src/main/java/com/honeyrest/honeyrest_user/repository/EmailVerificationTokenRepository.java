package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.honeyrest_user.entity.EmailVerificationToken;
import com.honeyrest.honeyrest_user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenValue(String tokenValue);

    List<EmailVerificationToken> findByUser(User user);

    void deleteByUser(User user);

}
