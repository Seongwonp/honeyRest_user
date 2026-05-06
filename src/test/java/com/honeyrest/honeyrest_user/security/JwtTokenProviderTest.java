package com.honeyrest.honeyrest_user.security;

import com.honeyrest.honeyrest_user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.*;

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
}
