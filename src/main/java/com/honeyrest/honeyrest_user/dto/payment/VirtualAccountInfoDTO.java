package com.honeyrest.honeyrest_user.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 토스 결제 응답에서 추출한 가상계좌 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualAccountInfoDTO {
    private String accountNumber;
    private String bank;
    private String accountHolder;
    private LocalDateTime dueDate;  // 입금기한
}

