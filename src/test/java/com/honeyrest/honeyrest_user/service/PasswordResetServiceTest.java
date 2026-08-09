package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.entity.PasswordResetToken;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.PasswordResetTokenRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository,
                tokenRepository,
                emailService,
                passwordEncoder
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
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        passwordResetService.resetPassword("valid-token", "new-password");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
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
        verify(tokenRepository, never()).delete(any());
    }
}
