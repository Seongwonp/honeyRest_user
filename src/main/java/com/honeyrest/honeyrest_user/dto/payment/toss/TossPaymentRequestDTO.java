package com.honeyrest.honeyrest_user.dto.payment.toss;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossPaymentRequestDTO {
    private String orderId; //고유 주문번호
    private int amount; // 결제 금액
    private String orderName; // 주문명 (숙소명 + 객실명)
    private String customerName; // 예약자 이름
    private String customerMobilePhone; // 예약자 전화번호
    private String successUrl; // 결제 성공 시 리디렉션 URL
    private String failUrl; // 결제 실패 시 리디렉션 URL
}
