package com.honeyrest.honeyrest_user.dto.payment.toss;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossPaymentResult {
    private String paymentKey;
    private String orderId;
    private String method;
    private String status;
    private BigDecimal amount;
    private String receiptUrl;
    private ReservationRequestDTO reservationInfo;
    private String couponName;
    private Boolean isEmailSend;

    private Map<String, Object> raw; //토스 응답 전체
}
