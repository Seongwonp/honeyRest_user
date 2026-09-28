package com.honeyrest.honeyrest_user.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 토스 결제 승인 호출이 타임아웃된 뒤 결제 상태를 조회해 정리한 결과.
 * <ul>
 *     <li>{@code reversed=true}: 토스 쪽에서는 승인(DONE)돼 있어 자동 취소했다 — 사용자는 과금되지 않는다.</li>
 *     <li>{@code reversed=false}: 승인되지 않았거나(조회 결과 없음/ABORTED 등) 상태를 확정하지 못했다.</li>
 * </ul>
 * 어느 쪽이든 예약은 만들어지지 않았으므로 응답 상태는 504 로 통일하고 메시지로 구분한다.
 */
@Getter
public class PaymentConfirmTimeoutException extends ApiException {

    private final boolean reversed;

    public PaymentConfirmTimeoutException(String message, boolean reversed) {
        this(message, reversed, HttpStatus.GATEWAY_TIMEOUT);
    }

    public PaymentConfirmTimeoutException(String message, boolean reversed, HttpStatus status) {
        super(message, status);
        this.reversed = reversed;
    }
}
