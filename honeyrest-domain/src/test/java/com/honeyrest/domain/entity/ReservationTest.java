package com.honeyrest.domain.entity;

import com.honeyrest.domain.type.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reservation 도메인 메서드(update/cancel/validateNew + 상태 전이 requestCancel/complete/changeStatus) 검증. */
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

    private Reservation withStatus(String status) {
        return Reservation.builder().reservationId(2L).status(status).build();
    }

    @Test
    void requestCancel_은_CONFIRMED_에서만_CANCEL_REQUEST_로_바꾸고_사유를_남긴다() {
        Reservation r = reservation();
        r.requestCancel("일정 변경");
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCEL_REQUEST);
        assertThat(r.getCancelReason()).isEqualTo("일정 변경");

        for (String s : new String[]{ReservationStatus.PENDING, ReservationStatus.CANCEL_REQUEST,
                ReservationStatus.COMPLETED, ReservationStatus.CANCELLED}) {
            Reservation other = withStatus(s);
            assertThatThrownBy(() -> other.requestCancel("x"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("확정된 예약만");
            assertThat(other.getStatus()).isEqualTo(s);
            assertThat(other.getCancelReason()).isNull();
        }
    }

    @Test
    void complete_는_CONFIRMED_에서만_COMPLETED_로_바꾼다() {
        Reservation r = reservation();
        r.complete();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.COMPLETED);

        Reservation requested = withStatus(ReservationStatus.CANCEL_REQUEST);
        assertThatThrownBy(requested::complete).isInstanceOf(IllegalStateException.class);
        assertThat(requested.getStatus()).isEqualTo(ReservationStatus.CANCEL_REQUEST);
    }

    @Test
    void cancel_은_취소_외_모든_상태에서_허용된다() {
        for (String s : new String[]{ReservationStatus.PENDING, ReservationStatus.CONFIRMED,
                ReservationStatus.CANCEL_REQUEST, ReservationStatus.COMPLETED, ReservationStatus.NO_SHOW}) {
            Reservation r = withStatus(s);
            r.cancel("관리자 취소");
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }
    }

    @Test
    void 호스트_취소_승인과_거부는_CANCEL_REQUEST_에서만_가능하다() {
        Reservation approved = withStatus(ReservationStatus.CANCEL_REQUEST);
        approved.approveCancelRequest("승인");
        assertThat(approved.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(approved.getCancelReason()).isEqualTo("승인");

        Reservation rejected = withStatus(ReservationStatus.CANCEL_REQUEST);
        rejected.rejectCancelRequest("기간 경과");
        assertThat(rejected.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(rejected.getCancelReason()).isEqualTo("[REJECT] 기간 경과");

        Reservation confirmed = reservation();
        assertThatThrownBy(() -> confirmed.approveCancelRequest("x")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> confirmed.rejectCancelRequest("x")).isInstanceOf(IllegalStateException.class);
        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void markNoShow_는_대기_확정에서만_가능하다() {
        Reservation pending = withStatus(ReservationStatus.PENDING);
        pending.markNoShow();
        assertThat(pending.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);

        Reservation completed = withStatus(ReservationStatus.COMPLETED);
        assertThatThrownBy(completed::markNoShow).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void changeStatus_는_전이표를_따르고_정규화한다() {
        Reservation r = withStatus(ReservationStatus.CANCEL_REQUEST);
        r.changeStatus(" confirmed ");   // 호스트 취소 거부
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        r.changeStatus(ReservationStatus.CONFIRMED); // 같은 상태는 변경 없음
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        r.changeStatus(ReservationStatus.NO_SHOW);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);

        assertThatThrownBy(() -> r.changeStatus(ReservationStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
    }

    @Test
    void changeStatus_는_모르는_상태와_빈_값을_거부한다() {
        Reservation r = reservation();
        assertThatThrownBy(() -> r.changeStatus("CANCELED")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> r.changeStatus(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void status_와_cancelReason_에는_public_setter_가_없다() {
        assertThat(java.util.Arrays.stream(Reservation.class.getMethods()).map(java.lang.reflect.Method::getName))
                .doesNotContain("setStatus", "setCancelReason");
    }
}
