package com.honeyrest.honeyrest_user.service.payment;

import java.util.Map;
import java.util.Optional;

/**
 * 토스페이먼츠 결제 API 호출 경계. 타임아웃 보상 같은 규칙은 {@link TossService} 에 둔다.
 * 구현은 {@link HttpTossClient}(기본)와 {@code e2e.E2eTossClient}(e2e·demo 스텁)다.
 * 2xx 가 아니면 {@code ApiException(400/502)}, 통신 실패는 {@code ApiException(504)} 를 던진다.
 */
public interface TossClient {

    /** 결제 승인 (POST /v1/payments/confirm). body: paymentKey, orderId, amount. */
    Map<String, Object> confirm(Map<String, Object> body);

    /** 결제 전액 취소 (POST /v1/payments/{paymentKey}/cancel). */
    Map<String, Object> cancel(String paymentKey, String cancelReason);

    /** 주문번호로 결제 조회 (GET /v1/payments/orders/{orderId}). 결제가 없으면(404) empty. */
    Optional<Map<String, Object>> findByOrderId(String orderId);

    /** 결제창 URL 생성 요청 (POST /v1/payments). */
    Map<String, Object> createPayment(Map<String, Object> body);
}
