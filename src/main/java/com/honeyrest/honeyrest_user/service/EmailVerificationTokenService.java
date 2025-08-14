package com.honeyrest.honeyrest_user.service;


import com.honeyrest.honeyrest_user.dto.email.EmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.email.ResendEmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.email.TokenStatusResponseDTO;
import com.honeyrest.honeyrest_user.entity.EmailVerificationToken;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.EmailVerificationTokenRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {

    private final JavaMailSender mailSender;
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

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

        String link = "https://honeyrest.com/api/user/email/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("HoneyRest 이메일 인증");
        message.setText("아래 링크를 클릭해 이메일 인증을 완료해 주세요:\n" + link);
        mailSender.send(message);
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
}