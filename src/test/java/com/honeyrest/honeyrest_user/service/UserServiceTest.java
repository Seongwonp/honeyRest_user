package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.user.LoginResponseDTO;
import com.honeyrest.honeyrest_user.dto.user.UserLoginRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.UserResponseDTO;
import com.honeyrest.honeyrest_user.dto.user.UserSignupRequestDTO;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.security.JwtTokenProvider;
import com.honeyrest.honeyrest_user.service.email.EmailVerificationTokenService;
import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import com.honeyrest.honeyrest_user.util.RefreshTokenCookieManager;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationTokenService emailService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private FileUploadUtil fileUploadUtil;
    @Mock private RefreshTokenCookieManager refreshTokenCookieManager;
    @Mock private HttpServletResponse httpResponse;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ── signup ──────────────────────────────────────────────

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        UserSignupRequestDTO dto = UserSignupRequestDTO.builder()
                .email("test@example.com")
                .password("password1234")
                .name("홍길동")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1234")).thenReturn("hashed");

        User saved = User.builder()
                .userId(1L)
                .email("test@example.com")
                .passwordHash("hashed")
                .name("홍길동")
                .role("USER")
                .status("ACTIVE")
                .isVerified(false)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponseDTO response = userService.signup(dto);

        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("홍길동");
        verify(emailService).sendVerificationEmail(any());
    }

    @Test
    @DisplayName("중복 이메일 회원가입 시 예외 발생")
    void signup_duplicateEmail() {
        UserSignupRequestDTO dto = UserSignupRequestDTO.builder()
                .email("dup@example.com")
                .password("pass")
                .name("중복")
                .build();
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.signup(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 가입된 이메일");
    }

    @Test
    @DisplayName("만 14세 미만 회원가입 거부")
    void signup_underageUser() {
        UserSignupRequestDTO dto = UserSignupRequestDTO.builder()
                .email("young@example.com")
                .password("password1234")
                .name("어린이")
                .birthDate("2020-01-01")
                .build();

        when(userRepository.existsByEmail("young@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.signup(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만 14세 미만");
    }

    // ── login ──────────────────────────────────────────────

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .passwordHash("hashed")
                .name("홍길동")
                .role("USER")
                .status("ACTIVE")
                .isVerified(true)
                .build();

        UserLoginRequestDTO dto = UserLoginRequestDTO.builder()
                .email("user@example.com")
                .password("rawPassword")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "hashed")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L, "USER")).thenReturn("access_token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh_token");
        when(refreshTokenCookieManager.create("refresh_token"))
                .thenReturn(ResponseCookie.from("refreshToken", "refresh_token").build());
        when(userRepository.save(any(User.class))).thenReturn(user);

        LoginResponseDTO response = userService.login(dto, httpResponse);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 예외 발생")
    void login_wrongPassword() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .passwordHash("hashed")
                .status("ACTIVE")
                .isVerified(true)
                .build();

        UserLoginRequestDTO dto = UserLoginRequestDTO.builder()
                .email("user@example.com")
                .password("wrong")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(dto, httpResponse))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    @DisplayName("탈퇴 계정 로그인 시 예외 발생")
    void login_deletedAccount() {
        User user = User.builder()
                .userId(1L)
                .email("gone@example.com")
                .passwordHash("hashed")
                .status("DELETED")
                .isVerified(true)
                .build();

        UserLoginRequestDTO dto = UserLoginRequestDTO.builder()
                .email("gone@example.com")
                .password("pass")
                .build();

        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> userService.login(dto, httpResponse))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("탈퇴한 계정");
    }

    @Test
    @DisplayName("미인증 계정 로그인 시 예외 발생")
    void login_notVerified() {
        User user = User.builder()
                .userId(1L)
                .email("unverified@example.com")
                .passwordHash("hashed")
                .status("ACTIVE")
                .isVerified(false)
                .build();

        UserLoginRequestDTO dto = UserLoginRequestDTO.builder()
                .email("unverified@example.com")
                .password("pass")
                .build();

        when(userRepository.findByEmail("unverified@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> userService.login(dto, httpResponse))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이메일 인증");
    }

    // ── changePassword ────────────────────────────────────

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_success() {
        User user = User.builder()
                .userId(1L)
                .passwordHash("oldHash")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "oldHash")).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertThatCode(() -> userService.changePassword(1L, "currentPass", "newPass"))
                .doesNotThrowAnyException();

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("현재 비밀번호 불일치 시 예외 발생")
    void changePassword_wrongCurrentPassword() {
        User user = User.builder().userId(1L).passwordHash("hash").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "wrong", "newPass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 비밀번호가 올바르지 않습니다");
    }

    // ── usePoint ──────────────────────────────────────────

    @Test
    @DisplayName("포인트 사용 성공")
    void usePoint_success() {
        User user = User.builder().userId(1L).point(5000).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertThatCode(() -> userService.usePoint(1L, 3000))
                .doesNotThrowAnyException();

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("포인트 부족 시 예외 발생")
    void usePoint_insufficientPoints() {
        User user = User.builder().userId(1L).point(500).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.usePoint(1L, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트");
    }
}
