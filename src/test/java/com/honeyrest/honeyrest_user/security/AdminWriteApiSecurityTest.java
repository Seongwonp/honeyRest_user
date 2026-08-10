package com.honeyrest.honeyrest_user.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-3 회귀 테스트: 배너/이벤트 쓰기 API는 더 이상 익명 사용자에게 열려 있지 않아야 한다.
 * 읽기(GET)는 비회원도 계속 볼 수 있어야 한다(공개 목록/상세).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminWriteApiSecurityTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void 익명_사용자는_이벤트를_생성할_수_없다() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/event/add", null, String.class);
        assertTrue(response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN,
                "익명 요청이 차단되지 않음: " + response.getStatusCode());
    }

    @Test
    void 익명_사용자는_배너를_생성할_수_없다() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/banner/add", null, String.class);
        assertTrue(response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN,
                "익명 요청이 차단되지 않음: " + response.getStatusCode());
    }

    @Test
    void 이벤트_목록_조회는_여전히_공개다() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/event/activeList", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
