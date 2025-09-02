package com.honeyrest.honeyrest_user.dto.reservation;

import com.honeyrest.honeyrest_user.dto.payment.PaymentDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationDetailDTO {
    // 예약 정보
    private Long reservationId;
    private String reservationCode; // 예약번호
    private String thumbnailUrl;
    private Long accommodationId;
    private String accommodationName;
    private String roomName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private String status;
    private boolean isReviewed;

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

    // 결제 상세정보
    private PaymentDetailDTO paymentDetailDTO;




}
