package com.honeyrest.honeyrest_user.service.email;


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
                      <body style="font-family: 'Pretendard', sans-serif; background-color: #f9f9f9; padding: 40px;">
                        <div style="max-width: 600px; margin: auto; background-color: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 32px;">
                          <h2 style="color: #ffb300; margin-bottom: 16px;">HoneyRest 이메일 인증</h2>
                          <p style="font-size: 16px; color: #333;">안녕하세요, <strong>HoneyRest</strong>를 이용해 주셔서 감사합니다.<br>아래 버튼을 클릭하여 이메일 인증을 완료해 주세요.</p>
                          <div style="margin: 24px 0;">
                            <a href="%s" style="display: inline-block; background-color: #ffb300; color: #fff; padding: 14px 28px; border-radius: 8px; text-decoration: none; font-weight: bold;">
                              이메일 인증하기
                            </a>
                          </div>
                          <p style="font-size: 14px; color: #999;">이 링크는 24시간 동안 유효합니다.<br>문의사항은 support@honeyrest.com 으로 연락 주세요.</p>
                        </div>
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


    public void sendEmailChangeToken(User user, String newEmail) {
        String token = UUID.randomUUID().toString();

        EmailVerificationToken tokenEntity = EmailVerificationToken.builder()
                .user(user)
                .tokenValue(token)
                .tokenType("EMAIL_CHANGE")
                .build();

        tokenRepository.save(tokenEntity);

        String link = baseUrl + "/verify-email-change?token=" + token;

        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(newEmail); // ✅ 여기 수정됨
            helper.setSubject("HoneyRest 이메일 변경 인증");

            String htmlContent = """
        <html>
          <body style="font-family: 'Pretendard', sans-serif; background-color: #f9f9f9; padding: 40px;">
            <div style="max-width: 600px; margin: auto; background-color: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 32px;">
              <h2 style="color: #ffb300; margin-bottom: 16px;">HoneyRest 이메일 변경 인증</h2>
              <p style="font-size: 16px; color: #333;">안녕하세요, <strong>HoneyRest</strong>를 이용해 주셔서 감사합니다.<br>아래 버튼을 클릭하여 이메일 변경 인증을 완료해 주세요.</p>
              <div style="margin: 24px 0;">
                <a href="%s" style="display: inline-block; background-color: #ffb300; color: #fff; padding: 14px 28px; border-radius: 8px; text-decoration: none; font-weight: bold;">
                  이메일 인증하기
                </a>
              </div>
              <p style="font-size: 14px; color: #999;">이 링크는 24시간 동안 유효합니다.<br>문의사항은 support@honeyrest.com 으로 연락 주세요.</p>
            </div>
          </body>
        </html>
        """.formatted(link);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("📨 이메일 변경 인증 메일 발송 완료: {}", newEmail);
        } catch (Exception e) {
            log.error("이메일 변경 인증 메일 전송 실패", e);
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