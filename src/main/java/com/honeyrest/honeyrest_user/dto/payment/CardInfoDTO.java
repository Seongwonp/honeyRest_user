package com.honeyrest.honeyrest_user.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 토스 결제 응답에서 추출한 카드 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardInfoDTO {
    private String company;           // issuerCode
    private String number;             // 마스킹된 카드번호
    private Integer installmentMonths;  // 할부 개월
}

