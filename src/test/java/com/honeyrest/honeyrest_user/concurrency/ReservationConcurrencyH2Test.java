package com.honeyrest.honeyrest_user.concurrency;

import com.honeyrest.domain.entity.Accommodation;
import com.honeyrest.domain.entity.Room;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.service.reservation.ReserveService;
import com.honeyrest.honeyrest_user.support.ConcurrencyResult;
import com.honeyrest.honeyrest_user.support.H2IntegrationTestSupport;
import com.honeyrest.honeyrest_user.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 생성의 객실 락 + 잠금 읽기 경로를 H2 에서 실행한다 (네이티브 FOR UPDATE 쿼리 호환성 확인 겸).
 * InnoDB 동작 검증은 {@link ReservationConcurrencyMySqlIntegrationTest}. H2 는 REPEATABLE READ 에서
 * FOR UPDATE 도 스냅샷을 읽어 InnoDB 와 다르므로 스냅샷 시나리오는 여기서 다루지 않는다.
 */
@DisplayName("예약 동시성: 객실 락 + 잠금 읽기 (H2)")
class ReservationConcurrencyH2Test extends H2IntegrationTestSupport {

    @Autowired private ReserveService reserveService;

    private ReservationRequestDTO request(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        return ReservationRequestDTO.builder()
                .roomId(roomId).checkIn(checkIn).checkOut(checkOut).guests(2)
                .guestName("홍길동").guestPhone("01012345678")
                .reservationCode(TestFixtures.unique("RES-H2-"))
                .originalPrice(new BigDecimal("100000.00"))
                .build();
    }

    @Test
    @DisplayName("total_rooms=1 객실에 동시에 6건 예약 → 1건만 성공, 나머지 409")
    void concurrentReservationsForSingleRoom() throws Exception {
        Accommodation acc = fixtures.accommodation(TestFixtures.unique("동시H2시"), "ACTIVE");
        Room room = fixtures.room(acc.getAccommodationId(), 1, 4, "ACTIVE");
        LocalDate checkIn = LocalDate.now().plusDays(10);

        ConcurrencyResult result = ConcurrencyResult.run(6, i -> reserveService.createReservation(
                request(room.getRoomId(), checkIn, checkIn.plusDays(2)), new BigDecimal("200000.00"), BigDecimal.ZERO));

        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.failures()).hasSize(5).allSatisfy(e -> assertThat(e)
                .isInstanceOf(ApiException.class)
                .extracting(x -> ((ApiException) x).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
