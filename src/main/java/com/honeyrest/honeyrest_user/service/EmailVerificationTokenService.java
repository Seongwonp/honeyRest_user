package com.honeyrest.honeyrest_user.service;


import com.honeyrest.honeyrest_user.dto.email.EmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.email.ResendEmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.email.TokenStatusResponseDTO;
import com.honeyrest.honeyrest_user.entity.EmailVerificationToken;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.EmailVerificationTokenRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.util.EmailSender;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {

    private final JavaMailSender mailSender;
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;

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

        try {
            String link = baseUrl + "/verify?token=" + token;

            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject("HoneyRest 이메일 인증");

            String htmlContent = """
                <html>
                    <body style="font-family: Arial, sans-serif; line-height:1.6;">
                        <h2 style="color:#ff9900;">HoneyRest 이메일 인증</h2>
                        <p>아래 버튼을 클릭하여 이메일 인증을 완료해 주세요.</p>
                        <a href="%s" style="display:inline-block; padding:10px 20px; background-color:#ff9900; color:#fff; text-decoration:none; border-radius:5px;">
                            이메일 인증
                        </a>
                        <p>감사합니다.</p>
                    </body>
                </html>
            """.formatted(link);

            helper.setText(htmlContent, true); // true = HTML
            mailSender.send(message);
        } catch (Exception e) {
            log.error("이메일 전송 실패", e);
        }

        if (mailSender instanceof JavaMailSenderImpl senderImpl) {
            log.info("📧 SMTP 사용자명: {}", senderImpl.getUsername());
            log.info("📧 SMTP 호스트: {}", senderImpl.getHost());
            log.info("📧 SMTP 포트: {}", senderImpl.getPort());
            log.info("📧 SMTP 프로퍼티: {}", senderImpl.getJavaMailProperties());
        } else {
            log.warn("⚠️ JavaMailSender가 JavaMailSenderImpl이 아님");
        }

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
}