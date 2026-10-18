package com.honeyrest.honeyrest_user.service;

import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.support.ConcurrencyResult;
import com.honeyrest.honeyrest_user.support.H2IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** PointService 의 조건부 UPDATE 가 실제 DB(H2)에서 이중 사용을 막는지 확인한다. */
@DisplayName("포인트: 원자적 차감 (H2)")
class PointAtomicUpdateH2Test extends H2IntegrationTestSupport {

    @Autowired private PointService pointService;

    @Test
    @DisplayName("같은 잔액을 본 두 요청이 각각 4,000P 를 쓰면 하나만 성공하고 잔액은 음수가 되지 않는다")
    void concurrentUseOnlyOneSucceeds() throws Exception {
        User user = fixtures.user(5000);

        ConcurrencyResult result = ConcurrencyResult.run(2, i -> transactionTemplate.executeWithoutResult(s -> {
            // 각 요청이 자기 트랜잭션에서 사용자 엔티티를 읽는다 (둘 다 5,000 으로 보인다)
            User loaded = entityManager.find(User.class, user.getUserId());
            pointService.use(loaded, 4000, "예약 시 포인트 사용", 100L + i);
        }));

        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.failures()).singleElement().isInstanceOf(ApiException.class);
        assertThat(fixtures.find(User.class, user.getUserId()).getPoint()).isEqualTo(1000);
    }

    @Test
    @DisplayName("이력의 balance 는 변동 후 DB 잔액이다")
    void historyBalanceMatchesDb() {
        User user = fixtures.user(3000);

        Integer balance = transactionTemplate.execute(s ->
                pointService.earn(entityManager.find(User.class, user.getUserId()), 700, "테스트 적립", null).getBalance());

        assertThat(balance).isEqualTo(3700);
        assertThat(fixtures.find(User.class, user.getUserId()).getPoint()).isEqualTo(3700);
    }
}
