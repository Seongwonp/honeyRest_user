package com.honeyrest.honeyrest_user.dto.reservation;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationCompleteDTO {
    // 예약 정보
    private Long reservationId;
    private String reservationCode; // 예약번호
    private String accommodationName;
    private String roomName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;

    // 사용자 정보
    private String guestName;
    private String guestPhone;

    // 결제 정보
    private String paymentMethod; // TOSS, CARD, BANK
    private String paymentStatus; // PAID, FAILED
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private String receiptUrl; // 영수증 링크 (토스 or 카드)

    // 기타
    private Boolean isEmailSent; // 이메일 전송 여부
    private Long couponId; // 쿠폰 아이디(선택)
    private String couponName;   // 적용된 쿠폰 이름 (선택)
    private Integer usedPoint;  // 사용된 포인트 (선택)
    private Integer remainingPoint; // 남은 포인트


}
