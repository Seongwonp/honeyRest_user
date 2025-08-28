package com.honeyrest.honeyrest_user.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationResponseDTO {
    /** 예약 고유 식별자 */
    private Long reservationId;
    // 예약 고유 번호 (결제시 생성)
    private String reservationNumber;
    /** 최종 결제 금액 */
    private BigDecimal finalPrice;
    /** 할인 금액 */
    private BigDecimal discountAmount;
    /** 예약 상태 (예: 예약 완료, 취소 등) */
    private String status;
    /** 예약 관련 메시지 (상태 설명 등) */
    private String message;

    // ==================== 결제 정보 필드 ====================
    /** 결제 고유 식별자 */
    private Long paymentId;
    /** 결제 수단 (예: 카드, 가상계좌 등) */
    private String paymentMethod;
    /** 결제 완료 여부 */
    private boolean paid;
    /** 결제 금액 */
    private BigDecimal amount;
    // ---- 결제 상세(PaymentDetail) 관련 ----
    /** 카드사 */
    private String cardCompany;
    /** 카드 번호 */
    private String cardNumber;
    /** 할부 개월 수 */
    private Integer installmentMonths;
    /** 가상계좌 번호 */
    private String virtualAccountNumber;
    /** 가상계좌 은행명 */
    private String virtualAccountBank;
    /** 가상계좌 예금주 */
    private String virtualAccountHolder;
    /** 가상계좌 만료일 */
    private java.time.LocalDate virtualAccountExpire;
    /** 토스 결제 키 */
    private String tossPaymentKey;
}
