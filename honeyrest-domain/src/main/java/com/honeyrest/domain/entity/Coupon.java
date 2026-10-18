package com.honeyrest.domain.entity;


import com.honeyrest.domain.type.CouponDiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "coupon")
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "name", nullable = false, length = 50)
    private String name; // 쿠폰명

    @Column(name = "code", length = 50, unique = true)
    private String code; // 고유 쿠폰 코드(자동 발급의 경우 null)

    @Column(name = "discount_type", nullable = false)
    private String discountType; // 할인 유형

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue; // 할인 값

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount; // 최소 주문 금액

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount; // 최대 할인 금액 [통합] 사용자 쪽 이름 maxOrderAmount → 컬럼명/호스트와 같은 maxDiscountAmount

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType; // 적용대상

    @Column(name = "target_id")
    private Long targetId; // 특정 숙소, 카테고리 ID(null 가능)

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active",nullable = false)
    private boolean isActive; // 활성 여부

    /**
     * 주문 금액(amount)에 이 쿠폰을 적용한 할인액.
     * <ul>
     *   <li>정률(RATE/PERCENT/PERCENTAGE): amount × discountValue / 100 (소수 둘째 자리 반올림)</li>
     *   <li>정액(FIXED/AMOUNT): discountValue</li>
     * </ul>
     * 그 뒤 maxDiscountAmount(있으면)와 amount 를 넘지 않도록 자르고, 음수면 0 으로 만든다.
     * 사용 가능 여부(기간·활성·최소 주문 금액)는 검사하지 않는다 — 호출자가 먼저 확인한다.
     *
     * @throws IllegalStateException 지원하지 않는 할인 유형 (과거처럼 정액으로 조용히 처리하지 않는다)
     */
    public BigDecimal calculateDiscount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("주문 금액이 없습니다.");
        }
        BigDecimal value = discountValue != null ? discountValue : BigDecimal.ZERO;

        BigDecimal discount = switch (CouponDiscountType.from(discountType)) {
            case RATE -> amount.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED -> value;
        };

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        return discount.max(BigDecimal.ZERO);
    }
}
