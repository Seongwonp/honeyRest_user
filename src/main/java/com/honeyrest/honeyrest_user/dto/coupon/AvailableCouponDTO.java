package com.honeyrest.honeyrest_user.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 예약 폼의 사용 가능 쿠폰 1건 (사용자에게 발급된 쿠폰 = user_coupon 행 기준).
 * <p>
 * 결제 요청(reservationInfo.userCouponId)에는 반드시 {@link #userCouponId} 를 보내야 한다.
 * {@link #couponId} 는 쿠폰 마스터(coupon) id 로 표시/참고용이다. 과거에는 couponId 만 내려줘
 * 프론트가 마스터 id 를 보냈고, 결제는 그 값을 user_coupon id 로 조회해 다른 쿠폰을 찾거나 실패했다.
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
