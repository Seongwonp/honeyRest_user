package com.honeyrest.honeyrest_user.support;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * H2(test 프로필) + 인메모리 Redis 로 실제 서비스/리포지토리/HTTP 흐름을 실행하는 테스트의 공통 설정.
 * 모든 하위 클래스가 같은 설정을 쓰므로 스프링 컨텍스트(와 H2 DB)를 공유한다 → 데이터는 {@link TestFixtures} 로 고유하게 만든다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(InMemoryRedisTestConfig.class)
public abstract class H2IntegrationTestSupport {

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected EntityManager entityManager;

    protected TestFixtures fixtures;

    @BeforeEach
    void setUpFixtures() {
        fixtures = new TestFixtures(transactionTemplate, entityManager);
    }
}
