package com.honeyrest.honeyrest_user.dto.reservation;

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
    private Long roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private String reservationCode; // 프론트에서 생성한 예약번호 (비회원 비밀번호 포함 가능)
    private String guestName;
    private String guestPhone;
    private String specialRequest;

    private Long couponId; // 선택
    private Long userId;   // 선택 (비회원이면 null)

    private Boolean isEmailSend; // 선택

    private BigDecimal originalPrice;   // 객실 원가
    private BigDecimal discountAmount;  // 할인 금액
    private String couponName;          // 쿠폰 이름

    private Integer usedPoint; // 사용된 포인트 (선택)


}
