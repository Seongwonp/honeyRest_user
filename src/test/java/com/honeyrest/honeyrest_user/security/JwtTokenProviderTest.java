package com.honeyrest.honeyrest_user.security;

import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-32-chars-long!!";
    private static final long ACCESS_EXPIRATION = 3_600_000L;   // 1시간
    private static final long REFRESH_EXPIRATION = 604_800_000L; // 7일

    @Mock
    private UserRepository userRepository;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtTokenProvider = new JwtTokenProvider(
                TEST_SECRET,
                ACCESS_EXPIRATION,
                REFRESH_EXPIRATION,
                userRepository
        );
    }

    @Test
    @DisplayName("AccessToken 생성 성공")
    void createAccessToken_success() {
        String token = jwtTokenProvider.createAccessToken(1L, "USER");

        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("유효한 AccessToken 검증 통과")
    void validateTokenOrThrow_valid() {
        String token = jwtTokenProvider.createAccessToken(1L, "USER");

        assertThatCode(() -> jwtTokenProvider.validateTokenOrThrow(token))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("만료된 AccessToken 검증 실패")
    void validateTokenOrThrow_expired() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                TEST_SECRET, 1L, REFRESH_EXPIRATION, userRepository
        );
        String token = shortLivedProvider.createAccessToken(1L, "USER");

        // 1ms 만료 토큰은 생성 직후 만료로 간주될 수 있음
        assertThatThrownBy(() -> {
            Thread.sleep(10);
            shortLivedProvider.validateTokenOrThrow(token);
        }).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("위변조된 AccessToken 검증 실패")
    void validateTokenOrThrow_tampered() {
        String token = jwtTokenProvider.createAccessToken(1L, "USER") + "tampered";

        assertThatThrownBy(() -> jwtTokenProvider.validateTokenOrThrow(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("AccessToken에서 userId 추출")
    void getUserId_success() {
        String token = jwtTokenProvider.createAccessToken(42L, "USER");

        Long userId = jwtTokenProvider.getUserId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    @DisplayName("RefreshToken은 UUID 형식")
    void createRefreshToken_isUuid() {
        String token = jwtTokenProvider.createRefreshToken();

        // UUID 형식: 8-4-4-4-12
        assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("서로 다른 userId는 다른 토큰 생성")
    void createAccessToken_differentUsersDifferentTokens() {
        String token1 = jwtTokenProvider.createAccessToken(1L, "USER");
        String token2 = jwtTokenProvider.createAccessToken(2L, "USER");

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("로그아웃/비밀번호 변경 이후 발급 시각(tokenValidAfter)보다 먼저 발급된 토큰은 거부된다")
    void getAuthentication_revokedToken() {
        String token = jwtTokenProvider.createAccessToken(1L, "USER");
        User user = User.builder()
                .userId(1L)
                .role("USER")
                .status("ACTIVE")
                .tokenValidAfter(LocalDateTime.now().plusSeconds(5)) // 토큰 발급 이후 시각
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("tokenValidAfter 이후 발급된 토큰은 인증에 성공한다")
    void getAuthentication_validToken() {
        User user = User.builder()
                .userId(1L)
                .role("USER")
                .status("ACTIVE")
                .tokenValidAfter(LocalDateTime.now().minusMinutes(1)) // 토큰 발급 이전 시각
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String token = jwtTokenProvider.createAccessToken(1L, "USER");

        assertThatCode(() -> jwtTokenProvider.getAuthentication(token))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("탈퇴한 계정의 토큰은 거부된다")
    void getAuthentication_deletedAccount() {
        String token = jwtTokenProvider.createAccessToken(1L, "USER");
        User user = User.builder()
                .userId(1L)
                .role("USER")
                .status("DELETED")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(token))
                .isInstanceOf(JwtException.class);
    }

    // ── 토큰 폐기(tokenValidAfter) 초 단위 경계 ─────────────────────

    private User activeUser() {
        return User.builder()
                .userId(1L)
                .role("USER")
                .status("ACTIVE")
                .passwordHash("oldHash")
                .build();
    }

    @Test
    @DisplayName("revokeExistingTokens는 tokenValidAfter를 '현재 시각 초 내림 + 1초'로 설정한다")
    void revokeExistingTokens_truncatesToSecondsPlusOne() {
        User user = activeUser();
        LocalDateTime before = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        user.revokeExistingTokens();

        LocalDateTime after = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        assertThat(user.getTokenValidAfter().getNano()).isZero();
        assertThat(user.getTokenValidAfter()).isBetween(before.plusSeconds(1), after.plusSeconds(1));
    }

    @Test
    @DisplayName("로그아웃 직전(같은 초 포함)에 발급된 토큰은 로그아웃 후 거부된다")
    void getAuthentication_tokenIssuedBeforeLogout_rejected() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        String tokenBeforeLogout = jwtTokenProvider.createAccessToken(user);

        user.revokeExistingTokens(); // 로그아웃 — 대부분 토큰 발급과 같은 초에 실행된다

        assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(tokenBeforeLogout))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("폐기된 토큰");
    }

    @Test
    @DisplayName("로그아웃 직후 같은 초에 재로그인해 발급된 토큰은 인증에 성공한다")
    void getAuthentication_reloginSameSecondAfterLogout_accepted() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        user.revokeExistingTokens();                                  // 로그아웃
        String reloginToken = jwtTokenProvider.createAccessToken(user); // 즉시 재로그인

        assertThatCode(() -> jwtTokenProvider.getAuthentication(reloginToken))
                .doesNotThrowAnyException();
        // 재발급(refresh) 경로도 같은 규칙을 따른다
        String reissued = jwtTokenProvider.reissueAccessToken(user);
        assertThatCode(() -> jwtTokenProvider.getAuthentication(reissued))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비밀번호 변경 시 기존 토큰은 무효화되고 새로 발급한 토큰은 유효하다")
    void changePassword_invalidatesExistingTokens() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        when(passwordEncoder.matches("currentPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "oldHash")).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");
        UserService userService = new UserService(
                userRepository, null, mock(com.honeyrest.honeyrest_user.service.RefreshTokenService.class),
                passwordEncoder, jwtTokenProvider, null, null);

        String oldToken = jwtTokenProvider.createAccessToken(user);
        userService.changePassword(1L, "currentPass", "newPass");

        assertThat(user.getTokenValidAfter()).isNotNull();
        assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(oldToken))
                .isInstanceOf(JwtException.class);
        String newToken = jwtTokenProvider.createAccessToken(user);
        assertThatCode(() -> jwtTokenProvider.getAuthentication(newToken))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("iat와 tokenValidAfter는 초 단위로 비교한다 (나노초는 무시)")
    void isIssuedBefore_secondResolution() {
        LocalDateTime validAfter = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
        java.util.Date sameSecond = java.util.Date.from(
                validAfter.plusNanos(999_000_000).atZone(java.time.ZoneId.systemDefault()).toInstant());
        java.util.Date previousSecond = java.util.Date.from(
                validAfter.minusNanos(1).atZone(java.time.ZoneId.systemDefault()).toInstant());

        assertThat(JwtTokenProvider.isIssuedBefore(sameSecond, validAfter)).isFalse();
        assertThat(JwtTokenProvider.isIssuedBefore(previousSecond, validAfter)).isTrue();
        // tokenValidAfter에 나노초가 남아 있어도(마이그레이션 이전 데이터) 같은 초의 iat는 거부하지 않는다
        assertThat(JwtTokenProvider.isIssuedBefore(
                java.util.Date.from(validAfter.atZone(java.time.ZoneId.systemDefault()).toInstant()),
                validAfter.plusNanos(500_000_000))).isFalse();
    }
}
