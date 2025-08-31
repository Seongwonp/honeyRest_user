package com.honeyrest.honeyrest_user.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDetailDTO {
    private String cardCompanyName;         // 카드사 이름 (매핑 필요)
    private String maskedCardNumber;        // 마스킹된 카드번호
    private Integer installmentMonths;      // 할부 개월 수

    private String virtualAccountBank;      // 가상계좌 은행명
    private String virtualAccountNumber;    // 마스킹된 계좌번호
    private String virtualAccountHolder;    // 예금주
    private LocalDateTime virtualAccountExpire; // 만료일
}
