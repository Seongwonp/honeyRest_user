package com.honeyrest.honeyrest_user.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * APP_CORS_ORIGINS(쉼표 구분) 파싱.
 */
class CorsOriginsParsingTest {

    @Test
    @DisplayName("공백·빈 값·끝의 슬래시·중복을 정리한다")
    void normalizes() {
        assertThat(SecurityConfig.parseOrigins(" https://a.sslip.io/ , ,http://localhost:5173,https://a.sslip.io"))
                .containsExactly("https://a.sslip.io", "http://localhost:5173");
    }

    @Test
    @DisplayName("null/빈 문자열은 빈 목록")
    void emptyInput() {
        assertThat(SecurityConfig.parseOrigins(null)).isEmpty();
        assertThat(SecurityConfig.parseOrigins("")).isEmpty();
    }
}
