package com.honeyrest.honeyrest_user.service.email;

import com.honeyrest.honeyrest_user.dto.email.EmailRequestDTO;
import com.honeyrest.domain.entity.EmailVerificationToken;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.repository.EmailVerificationTokenRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 가입 인증 토큰: 만료 · 1회용 · 용도 확인 · 발송 한도.
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenServiceTest {

    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private EmailRateLimiter emailRateLimiter;

    private EmailVerificationTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationTokenService(tokenRepository, userRepository, emailService, emailRateLimiter);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:5173");
        user = User.builder().userId(1L).email("a@example.com").isVerified(false).build();
    }

    private EmailVerificationToken token(String type, LocalDateTime expiry) {
        return EmailVerificationToken.builder()
                .user(user).tokenValue("tok").tokenType(type).expiryDate(expiry).build();
    }

    @Test
    void 유효한_토큰은_한번만_소비되고_사용자를_인증한다() {
        when(tokenRepository.findByTokenValue("tok")).thenReturn(Optional.of(token("SIGNUP", LocalDateTime.now().plusHours(1))));
        when(tokenRepository.consumeByTokenValue("tok")).thenReturn(1);

        assertThat(service.verifyToken("tok")).isTrue();
        assertThat(user.getIsVerified()).isTrue();
    }

    @Test
    void 이미_소비된_토큰은_거부한다() {
        when(tokenRepository.findByTokenValue("tok")).thenReturn(Optional.of(token("SIGNUP", LocalDateTime.now().plusHours(1))));
        when(tokenRepository.consumeByTokenValue("tok")).thenReturn(0);

        assertThatThrownBy(() -> service.verifyToken("tok"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용된 토큰");
        assertThat(user.getIsVerified()).isFalse();
    }

    @Test
    void 만료된_토큰은_소비하지_않고_거부한다() {
        when(tokenRepository.findByTokenValue("tok")).thenReturn(Optional.of(token("SIGNUP", LocalDateTime.now().minusMinutes(1))));

        assertThatThrownBy(() -> service.verifyToken("tok"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("만료");
        verify(tokenRepository, never()).consumeByTokenValue(any());
    }

    @Test
    void 이메일_변경_토큰으로는_가입_인증을_할_수_없다() {
        when(tokenRepository.findByTokenValue("tok")).thenReturn(Optional.of(token("EMAIL_CHANGE", LocalDateTime.now().plusHours(1))));

        assertThatThrownBy(() -> service.verifyToken("tok"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(tokenRepository, never()).consumeByTokenValue(any());
    }

    @Test
    void 인증메일_발송은_한도를_검사하고_이전_토큰을_무효화한다() {
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));

        service.sendVerificationEmail(new EmailRequestDTO("a@example.com"));

        verify(emailRateLimiter).checkAndRecord(EmailRateLimiter.Purpose.VERIFY, "a@example.com");
        verify(tokenRepository).deleteAllByUserAndTokenType(user, "SIGNUP");
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }
}
