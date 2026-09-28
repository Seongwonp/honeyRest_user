package com.honeyrest.honeyrest_user.service;

import com.honeyrest.domain.entity.PasswordResetToken;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.repository.PasswordResetTokenRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.service.email.EmailRateLimiter;
import com.honeyrest.honeyrest_user.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private EmailRateLimiter emailRateLimiter;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository,
                tokenRepository,
                emailService,
                passwordEncoder,
                refreshTokenService,
                emailRateLimiter
        );
    }

    @Test
    void requestReset_createsTokenAndSendsEmailForExistingUser() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        passwordResetService.requestReset("user@example.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenValue()).isNotBlank();
        assertThat(savedToken.getExpiryDate()).isAfter(LocalDateTime.now());
        verify(emailService).sendPasswordReset("user@example.com", savedToken.getTokenValue());
    }

    @Test
    void requestReset_returnsGenericResultForUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("unknown@example.com");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordReset(any(), any());
    }

    @Test
    void resetPassword_updatesPasswordAndConsumesToken() {
        User user = User.builder().userId(1L).passwordHash("old-hash").build();
        PasswordResetToken token = PasswordResetToken.create(
                user,
                "valid-token",
                LocalDateTime.now().plusMinutes(10)
        );
        when(tokenRepository.findByTokenValue("valid-token")).thenReturn(Optional.of(token));
        when(tokenRepository.consumeByTokenValue("valid-token")).thenReturn(1);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        passwordResetService.resetPassword("valid-token", "new-password");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
        verify(tokenRepository).consumeByTokenValue("valid-token");
    }

    @Test
    void resetPassword_revokesAccessAndRefreshTokens() {
        User user = User.builder().userId(1L).passwordHash("old-hash").build();
        PasswordResetToken token = PasswordResetToken.create(
                user,
                "valid-token",
                LocalDateTime.now().plusMinutes(10)
        );
        when(tokenRepository.findByTokenValue("valid-token")).thenReturn(Optional.of(token));
        when(tokenRepository.consumeByTokenValue("valid-token")).thenReturn(1);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        passwordResetService.resetPassword("valid-token", "new-password");

        // 기존 refresh token(DB 저장분) 삭제 + 기존 access token 폐기 시각 기록
        verify(refreshTokenService).invalidateAllByUser(user);
        assertThat(user.getTokenValidAfter()).isNotNull();
    }

    @Test
    void resetPassword_rejectsExpiredToken() {
        User user = User.builder().userId(1L).build();
        PasswordResetToken token = PasswordResetToken.create(
                user,
                "expired-token",
                LocalDateTime.now().minusMinutes(1)
        );
        when(tokenRepository.findByTokenValue("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "new-password"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("토큰이 만료되었습니다");

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).consumeByTokenValue(any());
        verify(refreshTokenService, never()).invalidateAllByUser(any());
    }

    @Test
    void requestReset_invalidatesPreviousTokensAndChecksRateLimit() {
        User user = User.builder().userId(1L).email("user@example.com").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        passwordResetService.requestReset("user@example.com");

        verify(emailRateLimiter).checkAndRecord(EmailRateLimiter.Purpose.PASSWORD_RESET, "user@example.com");
        verify(tokenRepository).deleteAllByUser(user);
    }

    @Test
    void requestReset_rateLimitAppliesEvenForUnknownEmail() {
        doThrow(new ApiException("요청이 너무 많습니다.", HttpStatus.TOO_MANY_REQUESTS))
                .when(emailRateLimiter).checkAndRecord(EmailRateLimiter.Purpose.PASSWORD_RESET, "unknown@example.com");

        assertThatThrownBy(() -> passwordResetService.requestReset("unknown@example.com"))
                .isInstanceOf(ApiException.class);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void resetPassword_rejectsAlreadyConsumedToken() {
        User user = User.builder().userId(1L).passwordHash("old-hash").build();
        PasswordResetToken token = PasswordResetToken.create(user, "used-token", LocalDateTime.now().plusMinutes(10));
        when(tokenRepository.findByTokenValue("used-token")).thenReturn(Optional.of(token));
        when(tokenRepository.consumeByTokenValue("used-token")).thenReturn(0); // 동시 요청이 먼저 소비함

        assertThatThrownBy(() -> passwordResetService.resetPassword("used-token", "new-password"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용된 토큰");
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
        verify(userRepository, never()).save(any());
    }
}
