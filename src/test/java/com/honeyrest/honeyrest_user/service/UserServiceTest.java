package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.user.LoginResult;
import com.honeyrest.honeyrest_user.dto.user.SocialLoginRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.UserLoginRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.UserResponseDTO;
import com.honeyrest.honeyrest_user.dto.user.UserSignupRequestDTO;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.security.JwtTokenProvider;
import com.honeyrest.honeyrest_user.service.email.EmailVerificationTokenService;
import com.honeyrest.honeyrest_user.storage.FileStorage;
import com.honeyrest.honeyrest_user.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
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
    @Mock private FileStorage fileStorage;

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
        when(jwtTokenProvider.createAccessToken(user)).thenReturn("access_token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh_token");
        when(userRepository.save(any(User.class))).thenReturn(user);

        LoginResult result = userService.login(dto);

        // 서비스는 쿠키를 만들지 않고 토큰과 응답 DTO 만 돌려준다 (쿠키는 AuthController 책임).
        assertThat(result.response().getAccessToken()).isEqualTo("access_token");
        assertThat(result.response().getUser().getEmail()).isEqualTo("user@example.com");
        assertThat(result.refreshToken()).isEqualTo("refresh_token");
        assertThat(result.toString()).doesNotContain("refresh_token");
        verify(refreshTokenService).invalidateAllByUser(user);
        verify(refreshTokenService).saveRefreshToken(eq(user), eq("refresh_token"), any());
        assertThat(user.getLastLogin()).isNotNull();
    }

    @Test
    @DisplayName("소셜 로그인: 처음이면 가입시키고 토큰을 발급한다")
    void socialLogin_createsUserAndIssuesTokens() {
        SocialLoginRequestDTO dto = SocialLoginRequestDTO.builder()
                .socialType("KAKAO").socialId("k-1").email("kakao@example.com").name("카카오")
                .build();
        when(userRepository.findBySocialTypeAndSocialId("KAKAO", "k-1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.createAccessToken(any(User.class))).thenReturn("access_token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh_token");

        LoginResult result = userService.socialLogin(dto);

        assertThat(result.response().getUser().getEmail()).isEqualTo("kakao@example.com");
        assertThat(result.response().getAccessToken()).isEqualTo("access_token");
        assertThat(result.refreshToken()).isEqualTo("refresh_token");
        verify(refreshTokenService).saveRefreshToken(any(User.class), eq("refresh_token"), any());
    }

    @Test
    @DisplayName("소셜 로그인: 탈퇴 계정이면 토큰을 발급하지 않는다")
    void socialLogin_deletedAccount() {
        User deleted = User.builder().userId(2L).status("DELETED").build();
        SocialLoginRequestDTO dto = SocialLoginRequestDTO.builder().socialType("GOOGLE").socialId("g-1").build();
        when(userRepository.findBySocialTypeAndSocialId("GOOGLE", "g-1")).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> userService.socialLogin(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("탈퇴한 계정");
        verifyNoInteractions(jwtTokenProvider, refreshTokenService);
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

        assertThatThrownBy(() -> userService.login(dto))
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

        assertThatThrownBy(() -> userService.login(dto))
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

        assertThatThrownBy(() -> userService.login(dto))
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
    @DisplayName("비밀번호 변경 시 해당 사용자의 refresh token(DB)이 모두 삭제되고 access token 폐기 시각이 기록된다")
    void changePassword_invalidatesRefreshTokens() {
        User user = User.builder()
                .userId(1L)
                .passwordHash("oldHash")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "oldHash")).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");

        userService.changePassword(1L, "currentPass", "newPass");

        verify(refreshTokenService).invalidateAllByUser(user);
        assertThat(user.getTokenValidAfter()).isNotNull();
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 refresh token을 삭제하지 않는다")
    void changePassword_wrongPassword_keepsRefreshTokens() {
        User user = User.builder().userId(1L).passwordHash("hash").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "wrong", "newPass"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(refreshTokenService, never()).invalidateAllByUser(any());
        assertThat(user.getTokenValidAfter()).isNull();
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

    // ── not found → 404 ─────────────────────────────────

    @Test
    @DisplayName("없는 사용자 조회·탈퇴는 404(NotFoundException)")
    void missingUserIsNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserInfo(9L))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((NotFoundException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> userService.deleteAccount(9L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> userService.changePassword(9L, "a", "b"))
                .isInstanceOf(NotFoundException.class);
    }
}
