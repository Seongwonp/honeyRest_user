package com.honeyrest.domain.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Coupon.calculateDiscount: 정률/정액 별칭, 상한, 모르는 유형 거부. */
class CouponTest {

    private static Coupon coupon(String type, String value, String max) {
        return Coupon.builder()
                .discountType(type)
                .discountValue(new BigDecimal(value))
                .maxDiscountAmount(max == null ? null : new BigDecimal(max))
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"RATE", "PERCENT", "PERCENTAGE", "percent", " Rate "})
    void 정률_별칭은_모두_퍼센트로_계산한다(String type) {
        assertThat(coupon(type, "10", null).calculateDiscount(new BigDecimal("123456")))
                .isEqualByComparingTo("12345");
    }

    @ParameterizedTest
    @ValueSource(strings = {"FIXED", "AMOUNT", "fixed"})
    void 정액_별칭은_할인값을_그대로_쓴다(String type) {
        assertThat(coupon(type, "5000", null).calculateDiscount(new BigDecimal("100000")))
                .isEqualByComparingTo("5000");
    }

    @Test
    void 최대_할인_금액과_주문_금액을_넘지_않는다() {
        assertThat(coupon("PERCENT", "50", "20000").calculateDiscount(new BigDecimal("100000")))
                .isEqualByComparingTo("20000");
        assertThat(coupon("FIXED", "30000", null).calculateDiscount(new BigDecimal("10000")))
                .isEqualByComparingTo("10000");
    }

    @Test
    void 정률은_원_단위_미만을_버린다_프론트_Math_floor_와_같다() {
        // 과거: 소수 둘째 자리 HALF_UP → 3333.30 이 되어 프론트(3,333원)와 결제 금액이 어긋났다.
        BigDecimal discount = coupon("PERCENT", "10", null).calculateDiscount(new BigDecimal("33333"));
        assertThat(discount).isEqualByComparingTo("3333");
        assertThat(discount.scale()).isZero();

        // 반올림이 아니라 버림 (99.99 → 99)
        assertThat(coupon("RATE", "3.333", null).calculateDiscount(new BigDecimal("3000")))
                .isEqualByComparingTo("99");
    }

    @Test
    void 정액도_원_단위_미만은_버린다() {
        assertThat(coupon("FIXED", "5000.90", null).calculateDiscount(new BigDecimal("100000")))
                .isEqualByComparingTo("5000");
    }

    @Test
    void 최소_주문_금액이_NULL_이면_제한이_없다() {
        Coupon noMin = Coupon.builder().discountType("FIXED").discountValue(BigDecimal.ONE).build();
        Coupon min = Coupon.builder().discountType("FIXED").discountValue(BigDecimal.ONE)
                .minOrderAmount(new BigDecimal("50000")).build();

        assertThat(noMin.meetsMinOrder(new BigDecimal("1"))).isTrue();
        assertThat(min.meetsMinOrder(new BigDecimal("49999"))).isFalse();
        assertThat(min.meetsMinOrder(new BigDecimal("50000"))).isTrue();
    }

    @Test
    void 적용_대상은_ALL_또는_해당_숙소만_허용한다() {
        assertThat(Coupon.builder().targetType("ALL").build().isApplicableToAccommodation(1L)).isTrue();
        assertThat(Coupon.builder().targetType("ACCOMMODATION").targetId(1L).build().isApplicableToAccommodation(1L)).isTrue();
        assertThat(Coupon.builder().targetType("ACCOMMODATION").targetId(2L).build().isApplicableToAccommodation(1L)).isFalse();
        assertThat(Coupon.builder().targetType("CATEGORY").build().isApplicableToAccommodation(1L)).isFalse();
    }

    @Test
    void 사용_기간과_활성_여부() {
        java.time.LocalDateTime now = java.time.LocalDateTime.of(2026, 10, 18, 12, 0);
        Coupon active = Coupon.builder().isActive(true)
                .startDate(now.minusDays(1)).endDate(now.plusDays(1)).build();
        assertThat(active.isUsableAt(now)).isTrue();
        assertThat(active.isUsableAt(now.plusDays(2))).isFalse();
        assertThat(Coupon.builder().isActive(false)
                .startDate(now.minusDays(1)).endDate(now.plusDays(1)).build().isUsableAt(now)).isFalse();
    }

    @Test
    void 음수_할인은_0으로_만든다() {
        assertThat(coupon("FIXED", "-100", null).calculateDiscount(new BigDecimal("1000")))
                .isEqualByComparingTo("0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"BOGUS", "", "FREE_NIGHT"})
    void 모르는_유형은_정액으로_대체하지_않고_예외(String type) {
        assertThatThrownBy(() -> coupon(type, "10", null).calculateDiscount(new BigDecimal("1000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("할인 유형");
    }

    @Test
    void 유형이_null_이어도_예외() {
        assertThatThrownBy(() -> coupon(null, "10", null).calculateDiscount(new BigDecimal("1000")))
                .isInstanceOf(IllegalStateException.class);
    }
}
