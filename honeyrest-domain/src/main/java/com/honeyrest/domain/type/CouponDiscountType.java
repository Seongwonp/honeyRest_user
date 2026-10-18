package com.honeyrest.domain.type;

import java.util.Locale;
import java.util.Set;

/**
 * 쿠폰 할인 유형 (coupon.discount_type VARCHAR). DB 에는 여러 철자가 섞여 있어 별칭으로 해석한다.
 * <ul>
 *   <li>{@link #RATE}: 정률(%) — "RATE", "PERCENT"(사용자 API 시드·프론트), "PERCENTAGE"(호스트 쿠폰 등록 화면)</li>
 *   <li>{@link #FIXED}: 정액(원) — "FIXED"(시드·프론트), "AMOUNT"(호스트 쿠폰 등록 화면)</li>
 * </ul>
 * 과거 사용자 API 는 RATE/PERCENT 가 아니면 모두 정액으로 계산해, 호스트가 만든 "PERCENTAGE" 쿠폰(예: 10%)이
 * 10원 할인으로 적용됐다. 이제 모르는 값은 조용히 정액으로 처리하지 않고 예외로 드러낸다.
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
