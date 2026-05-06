package com.honeyrest.honeyrest_user.service.email;


import com.honeyrest.honeyrest_user.dto.email.EmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.email.ResendEmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.email.TokenStatusResponseDTO;
import com.honeyrest.honeyrest_user.entity.EmailVerificationToken;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.EmailVerificationTokenRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(EmailRequestDTO requestDto) {
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자가 존재하지 않습니다."));

        String token = UUID.randomUUID().toString();

        EmailVerificationToken emailToken = EmailVerificationToken.builder()
                .user(user)
                .tokenValue(token)
                .tokenType("SIGNUP")
                .build();

        tokenRepository.save(emailToken);

        String link = baseUrl + "/verify?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), link);

    }

    @Transactional
    public boolean verifyToken(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("토큰이 만료되었습니다.");
        }

        token.getUser().verify(); // User 엔티티에 isVerified = true 처리
        tokenRepository.delete(token); // 인증 후 토큰 삭제

        return true;
    }

    public void resendVerificationEmail(ResendEmailRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // 이미 인증된 사용자라면 이메일 재전송하지 않음
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalStateException("이미 인증된 사용자입니다.");
        }

        // 기존 토큰 삭제
        tokenRepository.findAll().stream()
                .filter(t -> t.getUser().equals(user))
                .forEach(tokenRepository::delete);

        // 새 토큰 발송
        sendVerificationEmail(new EmailRequestDTO(dto.getEmail()));
    }

    public TokenStatusResponseDTO getTokenStatus(String tokenValue) {
        Optional<EmailVerificationToken> optional = tokenRepository.findByTokenValue(tokenValue);

        if (optional.isEmpty()) {
            return new TokenStatusResponseDTO(false, false, true);
        }

        EmailVerificationToken token = optional.get();
        boolean expired = token.getExpiryDate().isBefore(LocalDateTime.now());

        return new TokenStatusResponseDTO(true, token.getUser().getIsVerified(), expired);
    }


    public void sendEmailChangeToken(Long userId, String newEmail, boolean isPasswordVerified) {
        if (!isPasswordVerified) throw new SecurityException("비밀번호 인증 필요");
        if (userRepository.existsByEmail(newEmail)) throw new IllegalArgumentException("이미 사용 중인 이메일");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 기존 로직 유지
        String token = UUID.randomUUID().toString();
        EmailVerificationToken tokenEntity = EmailVerificationToken.builder()
                .user(user)
                .tokenValue(token)
                .tokenType("EMAIL_CHANGE")
                .build();

        tokenRepository.save(tokenEntity);

        String link = baseUrl + "/verify-email-change?token=" + token + "&newEmail=" + URLEncoder.encode(newEmail, StandardCharsets.UTF_8);
        emailService.sendEmailChangeToken(newEmail, link);
    }


    @Transactional
    public void confirmEmailChange(String tokenValue, String newEmail) {
        EmailVerificationToken token = tokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("토큰이 만료되었습니다.");
        }

        User user = token.getUser();
        user.updateEmail(newEmail); // User 엔티티에 메서드 추가
        tokenRepository.delete(token);

        userRepository.save(user);
        log.info("✅ 이메일 변경 완료: {}", newEmail);
    }


}