package com.honeyrest.honeyrest_user.e2e;

import com.honeyrest.honeyrest_user.service.payment.TossClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * e2e·demo 프로필 전용 토스 결제 스텁. 외부 호출 없이 승인은 항상 DONE, 취소는 {@link #cancellations()} 에 기록한다.
 * 금액·재고·중복 검증은 실제와 같이 PaymentOrchestrationService 가 수행한다.
 */
@Log4j2
@Component
@Profile({"e2e", "demo"})
public class E2eTossClient implements TossClient {

    /** 기록된 결제 취소 요청. */
    public record Cancellation(String paymentKey, String cancelReason, OffsetDateTime at) {
    }

    private final Map<String, Map<String, Object>> approvedByOrderId = new ConcurrentHashMap<>();
    private final List<Cancellation> cancellations = new CopyOnWriteArrayList<>();

    @Override
    public Map<String, Object> confirm(Map<String, Object> body) {
        String paymentKey = String.valueOf(body.get("paymentKey"));
        String orderId = String.valueOf(body.get("orderId"));
        Object amount = body.get("amount");

        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentKey", paymentKey);
        payment.put("orderId", orderId);
        payment.put("status", "DONE");
        payment.put("method", "카드");
        payment.put("totalAmount", amount);
        payment.put("approvedAt", OffsetDateTime.now().toString());
        payment.put("card", Map.of(
                "issuerCode", "61",
                "number", "12345678****000*",
                "installmentPlanMonths", 0
        ));
        payment.put("receipt", Map.of("url", "https://example.invalid/e2e-receipt/" + orderId));
        approvedByOrderId.put(orderId, payment);

        log.info("[e2e] 토스 승인 스텁: orderId={}, amount={}", orderId, amount);
        return payment;
    }

    @Override
    public Map<String, Object> cancel(String paymentKey, String cancelReason) {
        cancellations.add(new Cancellation(paymentKey, cancelReason, OffsetDateTime.now()));
        log.info("[e2e] 토스 취소 스텁 기록: paymentKey={}, reason={}", paymentKey, cancelReason);

        String orderId = approvedByOrderId.values().stream()
                .filter(p -> paymentKey.equals(p.get("paymentKey")))
                .map(p -> (String) p.get("orderId"))
                .findFirst()
                .orElse(null);
        Map<String, Object> response = new HashMap<>();
        response.put("paymentKey", paymentKey);
        response.put("orderId", orderId);
        response.put("status", "CANCELED");
        return response;
    }

    @Override
    public Optional<Map<String, Object>> findByOrderId(String orderId) {
        return Optional.ofNullable(approvedByOrderId.get(orderId));
    }

    @Override
    public Map<String, Object> createPayment(Map<String, Object> body) {
        return Map.of("paymentUrl", "https://example.invalid/e2e-payment/" + body.get("orderId"));
    }

    /** 지금까지 기록된 취소 요청 (e2e 검증용 조회 엔드포인트에서 사용). */
    public List<Cancellation> cancellations() {
        return new ArrayList<>(cancellations);
    }
}
