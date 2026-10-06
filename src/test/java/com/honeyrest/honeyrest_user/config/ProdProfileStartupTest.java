package com.honeyrest.honeyrest_user.config;

import com.honeyrest.honeyrest_user.service.email.EmailService;
import com.honeyrest.honeyrest_user.service.email.NoOpEmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영(prod) 프로필 설정 파일(application-prod.properties)이 "필수 환경변수만" 있는 상태에서
 * 해석되지 않는 placeholder 없이 컨텍스트를 띄우는지 검증한다.
 * <p>
 * - 필수: DB_URL / DB_USERNAME / DB_PASSWORD / JWT_SECRET / APP_BASE_URL 만 준다 (docker-compose 가 주는 값과 같은 이름).
 * - 선택(MAIL_*, TOSS_*, GOOGLE_*, KAKAO_*, WEATHER_API_KEY)은 일부러 주지 않는다 → 기동이 실패하면 안 된다.
 * - MySQL·Redis·Flyway 는 이 테스트 환경에 없으므로 H2 + Flyway 끔 + simple 캐시로만 바꾼다.
 *   (마이그레이션 자체는 integrationTest 의 FlywayMigrationMySqlIntegrationTest 가 MySQL 8.0 으로 검증한다)
 */
@SpringBootTest(properties = {
        // ---- 운영 compose 가 넣는 필수 환경변수 (값만 테스트용) ----
        "DB_URL=jdbc:h2:mem:honeyrest_prod_check;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=USER,VALUE,YEAR,MONTH,DAY;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "JWT_SECRET=prod-profile-test-jwt-secret-must-be-at-least-32-bytes",
        "APP_BASE_URL=https://203-0-113-10.sslip.io",
        "APP_STORAGE_LOCAL_DIR=${java.io.tmpdir}/honeyrest-prod-check-uploads",
        // ---- 이 테스트 환경에 없는 인프라만 대체 ----
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=simple",
        "management.health.redis.enabled=false"
})
@ActiveProfiles("prod")
@DisplayName("prod 프로필: 필수 환경변수만으로 기동하고, 선택 연동이 비어 있어도 실패하지 않는다")
class ProdProfileStartupTest {

    private static final String BASE_URL = "https://203-0-113-10.sslip.io";

    @Autowired private Environment env;
    @Autowired private EmailService emailService;
    @Autowired private ClientRegistrationRepository clientRegistrationRepository;
    @Autowired private CorsConfigurationSource corsConfigurationSource;

    @Test
    @DisplayName("MAIL_USERNAME 이 없으면 메일은 no-op 발송기로 대체된다")
    void mailFallsBackToNoOp() {
        assertThat(emailService).isInstanceOf(NoOpEmailService.class);
    }

    @Test
    @DisplayName("공개 주소 기반 값과 운영 보안 설정이 적용된다")
    void prodValuesResolved() {
        assertThat(env.getProperty("app.base-url")).isEqualTo(BASE_URL);
        assertThat(env.getProperty("kakao.redirect-uri")).isEqualTo(BASE_URL + "/login/kakao/callback");
        assertThat(env.getProperty("security.cookie.refresh.secure")).isEqualTo("true");
        assertThat(env.getProperty("spring.jpa.show-sql")).isEqualTo("false");
        assertThat(env.getProperty("app.storage.type")).isEqualTo("local");
        assertThat(env.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
        assertThat(env.getProperty("server.forward-headers-strategy")).isEqualTo("native");
    }

    @Test
    @DisplayName("OAuth 클라이언트 ID 미설정 시 더미 값으로 등록되어 기동이 실패하지 않는다")
    void oauthRegistrationsUseDummyWhenUnset() {
        assertThat(clientRegistrationRepository.findByRegistrationId("google").getClientId())
                .isEqualTo("not-configured");
        assertThat(clientRegistrationRepository.findByRegistrationId("kakao").getClientId())
                .isEqualTo("not-configured");
    }

    @Test
    @DisplayName("CORS 허용 Origin 기본값은 공개 주소 하나다")
    void corsDefaultsToBaseUrl() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accommodations");
        CorsConfiguration cors = corsConfigurationSource.getCorsConfiguration(request);
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly(BASE_URL);
    }
}
