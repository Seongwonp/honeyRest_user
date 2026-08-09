package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.entity.PasswordResetToken;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.PasswordResetTokenRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void requestReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // 계정 존재 여부가 외부 응답으로 노출되지 않도록 요청은 동일하게 처리한다.
            log.info("비밀번호 초기화 요청을 일반 응답으로 처리했습니다.");
            return;
        }

        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);

        PasswordResetToken resetToken = PasswordResetToken.create(user, tokenValue, expiry);

        tokenRepository.save(resetToken);
        emailService.sendPasswordReset(user.getEmail(), tokenValue);
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("토큰이 만료되었습니다");
        }

        User user = token.getUser();


        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(token); // 사용 후 즉시 삭제
        log.info("🔐 비밀번호 초기화 완료 및 토큰 삭제: userId={}", user.getUserId());
    }
}
