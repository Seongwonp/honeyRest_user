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
     * 주문 금액(amount)에 이 쿠폰을 적용한 할인액 (원 단위 정수).
     * <ul>
     *   <li>정률(RATE/PERCENT/PERCENTAGE): amount × discountValue / 100</li>
     *   <li>정액(FIXED/AMOUNT): discountValue</li>
     * </ul>
     * 그 뒤 maxDiscountAmount(있으면)와 amount 를 넘지 않도록 자르고, 음수면 0 으로 만든 다음
     * <b>원 단위 미만은 버린다(scale 0, RoundingMode.DOWN)</b>. 프론트엔드도 같은 규칙(Math.floor)으로 계산하므로
     * 과거처럼 서버(소수 둘째 자리 반올림)와 화면 금액이 달라 결제 금액 검증이 실패하지 않는다.
     * 예: 33,333원 × 10% = 3,333원.
     * 사용 가능 여부(기간·활성·최소 주문 금액·적용 대상)는 검사하지 않는다 — {@link #isUsableAt},
     * {@link #meetsMinOrder}, {@link #isApplicableToAccommodation} 으로 호출자가 먼저 확인한다.
     *
     * @throws IllegalStateException 지원하지 않는 할인 유형 (과거처럼 정액으로 조용히 처리하지 않는다)
     */
    public BigDecimal calculateDiscount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("주문 금액이 없습니다.");
        }
        BigDecimal value = discountValue != null ? discountValue : BigDecimal.ZERO;

        BigDecimal discount = switch (CouponDiscountType.from(discountType)) {
            case RATE -> amount.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            case FIXED -> value;
        };

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        return discount.max(BigDecimal.ZERO).setScale(0, RoundingMode.DOWN);
    }

    /** 활성 상태이고 now 가 사용 기간 [startDate, endDate] 안이면 true. */
    public boolean isUsableAt(LocalDateTime now) {
        return isActive
                && (startDate == null || !startDate.isAfter(now))
                && (endDate == null || !endDate.isBefore(now));
    }

    /** 최소 주문 금액 조건. minOrderAmount 가 NULL 이면 최소 금액 없음으로 본다. */
    public boolean meetsMinOrder(BigDecimal amount) {
        return minOrderAmount == null || (amount != null && amount.compareTo(minOrderAmount) >= 0);
    }

    /**
     * 이 숙소에 적용 가능한 쿠폰인지. 사용 가능 쿠폰 목록 조회(UserCouponQueryRepository)와 같은 규칙이다.
     * <ul>
     *   <li>targetType 은 ALL 또는 ACCOMMODATION 만 숙소 예약에 쓸 수 있다.</li>
     *   <li>targetId 가 있으면 그 숙소에만 적용된다 (NULL 이면 전체).</li>
     * </ul>
     */
    public boolean isApplicableToAccommodation(Long accommodationId) {
        if (!"ALL".equals(targetType) && !"ACCOMMODATION".equals(targetType)) {
            return false;
        }
        return targetId == null || targetId.equals(accommodationId);
    }
}
