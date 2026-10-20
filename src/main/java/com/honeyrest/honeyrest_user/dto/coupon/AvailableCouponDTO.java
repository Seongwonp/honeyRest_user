package com.honeyrest.honeyrest_user.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 예약 폼의 사용 가능 쿠폰 1건 (user_coupon 행 기준).
 * 결제 요청에는 {@link #userCouponId} 를 보낸다. {@link #couponId} 는 쿠폰 마스터 id 로 표시용이다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableCouponDTO {
    /** 사용자 쿠폰(user_coupon) id — 결제/예약 요청에 보내는 값. */
    private Long userCouponId;
    /** 쿠폰 마스터(coupon) id — 표시용. */
    private Long couponId;
    private String name;
    private String discountType; // PERCENT or FIXED
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxOrderAmount;
}
