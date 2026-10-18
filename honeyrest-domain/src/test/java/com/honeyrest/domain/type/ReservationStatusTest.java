package com.honeyrest.domain.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 공유 ReservationStatus: 두 앱이 같은 규칙을 쓰는지 도메인 모듈에서 직접 고정한다. */
class ReservationStatusTest {

    @Test
    void 재고_점유_상태는_취소를_제외한_다섯가지다() {
        assertThat(ReservationStatus.OCCUPYING)
                .containsExactly("PENDING", "CONFIRMED", "CANCEL_REQUEST", "COMPLETED", "NO_SHOW")
                .doesNotContain(ReservationStatus.CANCELLED);
        assertThat(ReservationStatus.isOccupying(" cancel_request ")).isTrue();
        assertThat(ReservationStatus.isOccupying("CANCELED")).isFalse();
        assertThat(ReservationStatus.isOccupying(null)).isFalse();
    }

    @Test
    void normalize_는_대문자로_정규화하고_모르는_값은_거부한다() {
        assertThat(ReservationStatus.normalize(" confirmed ", ReservationStatus.PENDING)).isEqualTo("CONFIRMED");
        assertThat(ReservationStatus.normalize("  ", ReservationStatus.PENDING)).isEqualTo("PENDING");
        assertThatThrownBy(() -> ReservationStatus.normalize("CANCELED", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "{0} → {1} 허용")
    @CsvSource({
            // 사용자 API
            "CONFIRMED, CANCEL_REQUEST",   // 사용자 취소 요청
            "CONFIRMED, COMPLETED",        // e2e 이용 완료 / 호스트 체크아웃 완료
            // 호스트
            "CANCEL_REQUEST, CANCELLED",   // 취소 승인
            "CANCEL_REQUEST, CONFIRMED",   // 취소 거부
            "PENDING, NO_SHOW",            // 노쇼
            "CONFIRMED, NO_SHOW",
            "PENDING, CONFIRMED",
            "PENDING, CANCELLED",          // 관리자 취소 (cancel)
            "CONFIRMED, CANCELLED",
            "COMPLETED, CANCELLED",
            "NO_SHOW, CANCELLED",
            "CANCELLED, CONFIRMED",        // 관리자 취소 복구 (재고 검사 동반)
            "confirmed, cancel_request"    // 대소문자 무시
    })
    void 허용된_전이(String from, String to) {
        assertThat(ReservationStatus.canTransition(from, to)).isTrue();
    }

    @ParameterizedTest(name = "{0} → {1} 거부")
    @CsvSource({
            "CANCEL_REQUEST, CANCEL_REQUEST", // 같은 상태
            "CANCEL_REQUEST, COMPLETED",
            "PENDING, CANCEL_REQUEST",        // 확정 전에는 취소 요청 불가
            "PENDING, COMPLETED",
            "COMPLETED, CONFIRMED",
            "COMPLETED, CANCEL_REQUEST",
            "NO_SHOW, CONFIRMED",
            "CANCELLED, PENDING",
            "CANCELLED, CANCELLED",
            "CANCELED, CONFIRMED",            // 모르는(오타) 상태
            "CONFIRMED, REFUNDED"
    })
    void 허용되지_않는_전이(String from, String to) {
        assertThat(ReservationStatus.canTransition(from, to)).isFalse();
    }

    @Test
    void null_은_전이할_수_없다() {
        assertThat(ReservationStatus.canTransition(null, ReservationStatus.CONFIRMED)).isFalse();
        assertThat(ReservationStatus.canTransition(ReservationStatus.CONFIRMED, null)).isFalse();
        assertThat(ReservationStatus.allowedTransitions(null)).isEmpty();
    }

    @Test
    void 모든_상태가_전이표에_있고_목표도_허용_목록_안에_있다() {
        for (String from : ReservationStatus.ALL) {
            assertThat(ReservationStatus.ALL).containsAll(ReservationStatus.allowedTransitions(from));
        }
        assertThat(ReservationStatus.allowedTransitions(ReservationStatus.CONFIRMED))
                .containsExactlyInAnyOrder("CANCEL_REQUEST", "COMPLETED", "NO_SHOW", "CANCELLED");
    }
}
