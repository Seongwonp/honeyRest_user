package com.honeyrest.honeyrest_user.dto.reservation;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationRequestDTO {

    @NotNull
    private Long roomId;

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    @NotNull
    @Min(1)
    private Integer guests;

    @NotBlank
    @Size(max = 100)
    private String reservationCode;

    @NotBlank
    @Size(max = 50)
    private String guestName;

    @NotBlank
    // 하이픈 유무 모두 허용 (회원 프로필의 010-1234-5678 형식이 결제 검증에서 거부되던 문제)
    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String guestPhone;

    @Size(max = 500)
    private String specialRequest;

    /**
     * 선택한 사용자 쿠폰(user_coupon) id. 예약 폼 availableCoupons[].userCouponId 값을 보낸다.
     * (과거 필드명 couponId 는 쿠폰 마스터 id 와 혼동돼 제거했다. 구 클라이언트가 couponId 를 보내면 무시되고,
     * 쿠폰 없이 다시 계산한 금액이 결제 금액과 달라 토스 승인 전에 400 으로 거절된다 — 과금 없음.)
     */
    private Long userCouponId;
    private Long userId;

    private Boolean isEmailSend;

    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private String couponName;

    @Min(0)
    private Integer usedPoint;

}
