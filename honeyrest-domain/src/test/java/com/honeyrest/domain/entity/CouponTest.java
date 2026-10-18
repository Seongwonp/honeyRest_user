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
                .isEqualByComparingTo("12345.60");
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
    void 정률은_소수_둘째_자리에서_반올림한다() {
        assertThat(coupon("RATE", "3.333", null).calculateDiscount(new BigDecimal("100")))
                .isEqualByComparingTo("3.33");
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
