package com.honeyrest.honeyrest_user.e2e;

import com.honeyrest.honeyrest_user.service.email.EmailService;
import com.honeyrest.honeyrest_user.service.payment.HttpTossClient;
import com.honeyrest.honeyrest_user.service.payment.TossClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * e2e 전용 빈(테스트 보조 엔드포인트, 토스 스텁, 메일 no-op, 인메모리 Redis)이
 * e2e 가 아닌 프로필에는 절대 등록되지 않는지 확인한다. 운영 배선이 그대로인지도 함께 본다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("e2e 전용 빈은 다른 프로필에 등록되지 않는다")
class E2eProfileIsolationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("e2e 보조 컨트롤러·스텁·인메모리 Redis 설정이 없다")
    void e2eBeansAbsent() {
        assertThat(context.getBeanNamesForType(E2eSupportController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(E2eTossClient.class)).isEmpty();
        assertThat(context.getBeanNamesForType(E2eNoOpEmailService.class)).isEmpty();
        assertThat(context.getBeanNamesForType(E2eRedisConfig.class)).isEmpty();
        assertThat(context.getBeanNamesForType(InMemoryRedisTemplate.class)).isEmpty();
    }

    @Test
    @DisplayName("토스 클라이언트는 실제 HTTP 구현, 메일은 원래 EmailService 다")
    void productionWiring() {
        assertThat(context.getBean(TossClient.class)).isInstanceOf(HttpTossClient.class);
        assertThat(context.getBean(EmailService.class)).isNotInstanceOf(E2eNoOpEmailService.class);
    }
}
