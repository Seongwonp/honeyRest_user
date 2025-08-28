package com.honeyrest.honeyrest_user.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResultDTO {
    private boolean success;      // 결제 성공 여부
    private String message;       // 성공/실패 메시지
    private Long paymentId;       // 결제 성공 시 생성된 Payment 엔티티 ID (선택)
}
