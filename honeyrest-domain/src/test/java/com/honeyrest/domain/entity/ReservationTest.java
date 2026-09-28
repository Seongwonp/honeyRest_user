package com.honeyrest.domain.entity;

import com.honeyrest.domain.type.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 호스트 사본에서 합친 Reservation 도메인 메서드(update/cancel/validateNew) 검증. */
class ReservationTest {

    private static final LocalDate IN = LocalDate.of(2026, 10, 1);
    private static final LocalDate OUT = LocalDate.of(2026, 10, 3);

    private Reservation reservation() {
        return Reservation.builder()
                .reservationId(1L)
                .user(User.builder().userId(1L).build())
                .room(Room.builder().roomId(1L).build())
                .accommodation(Accommodation.builder().accommodationId(1L).build())
                .accommodationName("허니 호텔").roomName("디럭스").reservationNumber("R-1")
                .checkInDate(IN).checkOutDate(OUT).guestCount(2).guestName("홍길동").guestPhone("010")
                .price(new BigDecimal("100000.00")).status(ReservationStatus.CONFIRMED)
                .build();
    }

    /** 테스트용 변경값: 지정한 필드 외에는 null(=변경 없음). */
    private record Changes(LocalDate checkInDate, LocalDate checkOutDate, Integer guestCount, String status)
            implements ReservationChanges {
        public LocalDate getCheckInDate() { return checkInDate; }
        public LocalDate getCheckOutDate() { return checkOutDate; }
        public Integer getGuestCount() { return guestCount; }
        public String getGuestName() { return null; }
        public String getGuestPhone() { return null; }
        public BigDecimal getPrice() { return null; }
        public String getStatus() { return status; }
        public String getCancelReason() { return null; }
        public String getSpecialRequest() { return null; }
        public String getAccommodationName() { return null; }
    }

    @Test
    void update_는_null_이_아닌_값만_반영한다() {
        Reservation r = reservation();
        r.update(new Changes(null, null, 4, null), null, null, null);

        assertThat(r.getGuestCount()).isEqualTo(4);
        assertThat(r.getGuestName()).isEqualTo("홍길동");
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(r.getCheckInDate()).isEqualTo(IN);
    }

    @Test
    void update_는_체크인이_체크아웃보다_늦으면_거부한다() {
        Reservation r = reservation();
        assertThatThrownBy(() -> r.update(new Changes(OUT.plusDays(1), null, null, null), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancel_은_상태와_사유를_바꾸고_중복_취소는_거부한다() {
        Reservation r = reservation();
        r.cancel("일정 변경");

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(r.getCancelReason()).isEqualTo("일정 변경");
        assertThatThrownBy(() -> r.cancel("again")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateNew_는_필수값_누락을_거부한다() {
        reservation().validateNew();
        Reservation missingName = Reservation.builder().status(ReservationStatus.PENDING).build();
        assertThatThrownBy(missingName::validateNew).isInstanceOf(IllegalStateException.class);
    }
}
