package com.honeyrest.domain.type;

import java.util.Locale;
import java.util.Set;

/**
 * 쿠폰 할인 유형 (coupon.discount_type). DB 에 여러 철자가 섞여 있어 별칭으로 해석한다.
 * RATE: "RATE", "PERCENT", "PERCENTAGE" / FIXED: "FIXED", "AMOUNT". 모르는 값은 예외로 드러낸다.
 */
public enum CouponDiscountType {

    RATE(Set.of("RATE", "PERCENT", "PERCENTAGE")),
    FIXED(Set.of("FIXED", "AMOUNT"));

    private final Set<String> aliases;

    CouponDiscountType(Set<String> aliases) {
        this.aliases = aliases;
    }

    /**
     * DB 문자열을 할인 유형으로 해석한다 (대소문자·앞뒤 공백 무시).
     *
     * @throws IllegalStateException null/빈 값 또는 지원하지 않는 유형 (쿠폰 데이터 오류)
     */
    public static CouponDiscountType from(String value) {
        if (value != null) {
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            for (CouponDiscountType type : values()) {
                if (type.aliases.contains(normalized)) {
                    return type;
                }
            }
        }
        throw new IllegalStateException("지원하지 않는 쿠폰 할인 유형입니다: " + value);
    }
}
