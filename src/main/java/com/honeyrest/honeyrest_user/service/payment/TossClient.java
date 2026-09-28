package com.honeyrest.honeyrest_user.service.payment;

import java.util.Map;
import java.util.Optional;

/**
 * 토스페이먼츠 결제 API 호출 경계.
 * <p>
 * {@link TossService} 는 이 인터페이스만 보고 승인/조회/취소를 호출하며, 타임아웃 보상 같은 비즈니스 규칙은
 * TossService 에 남긴다. 구현체는 프로필로 하나만 등록된다.
 * <ul>
 *     <li>{@link HttpTossClient} — 기본(운영/로컬/test) 프로필. 실제 토스 API 를 RestTemplate 으로 호출한다.</li>
 *     <li>{@code e2e.E2eTossClient} — {@code e2e} 프로필 전용 스텁. 외부 호출 없이 항상 승인한다.</li>
 * </ul>
 * 응답은 토스 API 의 JSON 바디를 그대로 담은 Map 이다.
 * 실패 규약: 토스가 2xx 가 아닌 응답을 주면 {@code ApiException(400/502)}, 통신 실패(타임아웃 등)는
 * {@code ApiException(504 GATEWAY_TIMEOUT)} 을 던진다. TossService 는 504 를 보고 보상 절차를 밟는다.
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
