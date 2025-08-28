package com.honeyrest.honeyrest_user.dto.payment.toss;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossPaymentUrlDTO {
    private String paymentUrl;
}