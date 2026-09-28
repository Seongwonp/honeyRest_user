package com.honeyrest.domain.type;

import org.junit.jupiter.api.Test;

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
}
