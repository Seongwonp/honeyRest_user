package com.honeyrest.honeyrest_user.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserController 의 {@code principal == null} 분기를 지운 근거: {@code /api/user/**} 는 SecurityConfig 에서
 * 인증 필수라 익명 요청은 컨트롤러에 닿기 전에 401 이 된다 (NPE → 500 이 되지 않는다).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("/api/user/** 는 익명 요청을 컨트롤러 전에 401 로 막는다")
class UserApiAuthenticationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "GET, /api/user/info",
            "GET, /api/user/coupons",
            "GET, /api/user/inquiries/List",
            "GET, /api/user/inquiries/counts",
            "GET, /api/user/inquiries/1",
            "DELETE, /api/user/inquiries/delete/1",
            "DELETE, /api/user/delete-account",
            "GET, /api/user/point-history"
    })
    void anonymousIsUnauthorized(String method, String path) {
        ResponseEntity<String> response = restTemplate.exchange(path, HttpMethod.valueOf(method), null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
