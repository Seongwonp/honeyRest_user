package com.honeyrest.honeyrest_user.service.reservation;

import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.room.RoomRepository;
import com.honeyrest.honeyrest_user.service.CancellationPolicyService;
import com.honeyrest.honeyrest_user.service.coupon.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("StayDatePolicy: 지난 날짜·역순·최대 숙박 일수 (Asia/Seoul 기준)")
class StayDatePolicyTest {

    /** UTC 2026-10-18 16:00 = 서울 2026-10-19 01:00 (서울은 이미 다음 날). */
    private static final Clock UTC_AFTERNOON = Clock.fixed(Instant.parse("2026-10-18T16:00:00Z"), ZoneId.of("UTC"));

    @Test
    void 오늘_서울_기준_체크인은_허용하고_어제는_거절한다() {
        LocalDate seoulToday = LocalDate.of(2026, 10, 19);

        assertThatCode(() -> StayDatePolicy.validate(seoulToday, seoulToday.plusDays(1), UTC_AFTERNOON))
                .doesNotThrowAnyException();
        // 서버 시간대(UTC)로는 18일이 "오늘"이지만 서울 기준으로는 지난 날짜다.
        assertThatThrownBy(() -> StayDatePolicy.validate(LocalDate.of(2026, 10, 18), seoulToday, UTC_AFTERNOON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("체크인 날짜는 오늘(2026-10-19)");
    }

    @Test
    void 체크아웃은_체크인보다_뒤여야_한다() {
        LocalDate d = LocalDate.of(2026, 11, 1);
        assertThatThrownBy(() -> StayDatePolicy.validate(d, d, UTC_AFTERNOON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("체크아웃 날짜는 체크인 날짜보다 뒤");
        assertThatThrownBy(() -> StayDatePolicy.validate(d, d.minusDays(1), UTC_AFTERNOON))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 최대_30박() {
        LocalDate d = LocalDate.of(2026, 11, 1);
        assertThatCode(() -> StayDatePolicy.validate(d, d.plusDays(30), UTC_AFTERNOON)).doesNotThrowAnyException();
        assertThatThrownBy(() -> StayDatePolicy.validate(d, d.plusDays(31), UTC_AFTERNOON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 30박");
    }

    @Test
    void 날짜_누락은_거절() {
        assertThatThrownBy(() -> StayDatePolicy.validate(null, LocalDate.of(2026, 11, 1), UTC_AFTERNOON))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 예약_폼_조회도_지난_날짜를_DB_조회_전에_400으로_거절한다() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        ReserveInfoService service = new ReserveInfoService(roomRepository, mock(UserRepository.class),
                mock(CancellationPolicyService.class), mock(CouponService.class), mock(PriceCalculator.class));
        LocalDate yesterday = StayDatePolicy.today().minusDays(1);

        assertThatThrownBy(() -> service.getFormInfo(1L, null, yesterday, yesterday.plusDays(1), 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("체크인 날짜는 오늘");
        verifyNoInteractions(roomRepository);
    }
}
