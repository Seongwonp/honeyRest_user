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
    private final EmailRateLimiter emailRateLimiter;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public void sendVerificationEmail(EmailRequestDTO requestDto) {
        emailRateLimiter.checkAndRecord(EmailRateLimiter.Purpose.VERIFY, requestDto.getEmail());
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자가 존재하지 않습니다."));
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalStateException("이미 인증된 사용자입니다.");
        }

        // 이전 가입 인증 링크는 무효화한다 (가장 최근 메일의 링크만 유효).
        tokenRepository.deleteAllByUserAndTokenType(user, "SIGNUP");

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
        // EMAIL_CHANGE 토큰으로 가입 인증을 처리하지 않는다. (tokenType 이 없는 과거 행은 SIGNUP 으로 간주)
        if (token.getTokenType() != null && !"SIGNUP".equals(token.getTokenType())) {
            throw new IllegalArgumentException("가입 인증용 토큰이 아닙니다.");
        }
        // 원자적 소비(1회용): 동시에 같은 링크를 두 번 열어도 한쪽만 성공한다.
        if (tokenRepository.consumeByTokenValue(tokenValue) == 0) {
            throw new IllegalStateException("이미 사용된 토큰입니다.");
        }

        token.getUser().verify(); // User 엔티티에 isVerified = true 처리

        return true;
    }

    @Transactional
    public void resendVerificationEmail(ResendEmailRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // 이미 인증된 사용자라면 이메일 재전송하지 않음
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalStateException("이미 인증된 사용자입니다.");
        }

        // 새 토큰 발송 (이전 SIGNUP 토큰 무효화와 발송 한도 검사는 sendVerificationEmail 이 처리한다.
        // 과거에는 findAll() 로 전체 토큰 테이블을 읽어 걸렀다.)
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


    @Transactional
    public void sendEmailChangeToken(Long userId, String newEmail, boolean isPasswordVerified) {
        if (!isPasswordVerified) throw new SecurityException("비밀번호 인증 필요");
        emailRateLimiter.checkAndRecord(EmailRateLimiter.Purpose.EMAIL_CHANGE, newEmail);
        if (userRepository.existsByEmail(newEmail)) throw new IllegalArgumentException("이미 사용 중인 이메일");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 이전 이메일 변경 링크는 무효화한다.
        tokenRepository.deleteAllByUserAndTokenType(user, "EMAIL_CHANGE");

        String token = UUID.randomUUID().toString();
        EmailVerificationToken tokenEntity = EmailVerificationToken.builder()
                .user(user)
                .tokenValue(token)
                .tokenType("EMAIL_CHANGE")
                .pendingEmail(newEmail)
                .build();

        tokenRepository.save(tokenEntity);

        String link = baseUrl + "/verify-email-change?token=" + token + "&newEmail=" + URLEncoder.encode(newEmail, StandardCharsets.UTF_8);
        emailService.sendEmailChangeToken(newEmail, link);
    }


    /**
     * requestingUserId는 인증된 호출자의 id다. 과거에는 (1) 토큰 종류를 확인하지 않아 SIGNUP 토큰으로도
     * 이메일을 바꿀 수 있었고, (2) 승인된 이메일을 서버에 저장하지 않아 newEmail 파라미터를 신뢰했으며,
     * (3) 엔드포인트가 인증 없이 열려 있었다(P0-8). 넷 다 여기서 막는다.
     */
    @Transactional
    public void confirmEmailChange(Long requestingUserId, String tokenValue, String newEmail) {
        EmailVerificationToken token = tokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("토큰이 만료되었습니다.");
        }
        if (!"EMAIL_CHANGE".equals(token.getTokenType())) {
            throw new IllegalArgumentException("이메일 변경용 토큰이 아닙니다.");
        }
        if (requestingUserId == null || !requestingUserId.equals(token.getUser().getUserId())) {
            throw new SecurityException("본인 계정의 이메일만 변경할 수 있습니다.");
        }
        if (token.getPendingEmail() == null || !token.getPendingEmail().equals(newEmail)) {
            throw new IllegalArgumentException("요청한 이메일과 토큰이 승인한 이메일이 일치하지 않습니다.");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (tokenRepository.consumeByTokenValue(tokenValue) == 0) {
            throw new IllegalStateException("이미 사용된 토큰입니다.");
        }

        User user = token.getUser();
        user.updateEmail(newEmail); // User 엔티티에 메서드 추가

        userRepository.save(user);
        log.info("✅ 이메일 변경 완료: userId={}", user.getUserId());
    }


}