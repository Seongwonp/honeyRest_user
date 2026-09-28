package com.honeyrest.honeyrest_user.service.payment;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentRequestDTO;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentUrlDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.exception.PaymentConfirmTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class TossService {

    /**
     * 토스 API 호출 경계. 기본 프로필은 실제 API 를 호출하는 {@link HttpTossClient},
     * e2e 프로필은 항상 승인하는 스텁이 주입된다 (운영 배선은 그대로).
     */
    private final TossClient tossClient;

    /**
     * 토스 결제 승인 (POST /v1/payments/confirm). 이 호출이 성공하면 실제 과금이 일어난다.
     * 토스가 2xx 가 아닌 응답을 주면 토스 에러 메시지를 담은 ApiException 을 던진다.
     * 응답을 받지 못하면(타임아웃) 결제 상태를 조회해 승인된 결제는 취소하고
     * {@link PaymentConfirmTimeoutException} 을 던진다 ({@link #reconcileTimedOutConfirm}).
     */
    public TossPaymentResult confirmPayment(TossConfirmRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", request.getOrderId());
        body.put("amount", request.getAmount());
        body.put("paymentKey", request.getPaymentKey());

        Map<String, Object> responseMap;
        try {
            responseMap = tossClient.confirm(body);
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.GATEWAY_TIMEOUT) {
                // 요청은 토스에 도달해 승인됐는데 응답만 못 받았을 수 있다. 결제 상태를 조회해 정리한다.
                throw reconcileTimedOutConfirm(request.getOrderId());
            }
            throw e;
        }

        log.info("토스 결제 승인 응답 수신: orderId={}, status={}",
                responseMap.get("orderId"), responseMap.get("status"));

        BigDecimal amount = responseMap.get("totalAmount") != null
                ? new BigDecimal(responseMap.get("totalAmount").toString())
                : BigDecimal.ZERO;

        String receiptUrl = null;
        Object receipt = responseMap.get("receipt");
        if (receipt instanceof Map<?, ?> receiptMap && receiptMap.get("url") != null) {
            receiptUrl = receiptMap.get("url").toString();
        } else if (responseMap.get("receiptUrl") != null) {
            receiptUrl = responseMap.get("receiptUrl").toString();
        }

        ReservationRequestDTO reservationInfo = request.getReservationInfo();
        TossPaymentResult result = TossPaymentResult.builder()
                .paymentKey((String) responseMap.get("paymentKey"))
                .orderId((String) responseMap.get("orderId"))
                .method((String) responseMap.get("method"))
                .status((String) responseMap.get("status"))
                .amount(amount)
                .receiptUrl(receiptUrl)
                .reservationInfo(reservationInfo)   // 프론트에서 받은 예약정보 유지
                .couponName(reservationInfo != null && reservationInfo.getCouponId() != null ? "쿠폰적용" : null)
                .isEmailSend(reservationInfo != null ? reservationInfo.getIsEmailSend() : null)
                .raw(responseMap)
                .build();
        // raw/reservationInfo에는 카드·구매자 정보와 예약자 실명·전화번호가 들어있어 통째로 로깅하지 않는다.
        log.info("최종 TossPaymentResult: orderId={}, status={}, paymentKey={}",
                result.getOrderId(), result.getStatus(), result.getPaymentKey());
        return result;
    }

    /**
     * 승인된 결제 전액 취소 (POST /v1/payments/{paymentKey}/cancel).
     * 승인 후 예약 저장이 실패했을 때 보상 트랜잭션으로 사용한다.
     */
    public void cancelPayment(String paymentKey, String cancelReason) {
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalArgumentException("취소할 paymentKey 가 없습니다.");
        }
        Map<String, Object> responseMap = tossClient.cancel(paymentKey, cancelReason);
        log.info("토스 결제 취소 완료: orderId={}, status={}", responseMap.get("orderId"), responseMap.get("status"));
    }

    /**
     * 승인 호출 타임아웃 보상.
     * {@code GET /v1/payments/orders/{orderId}} 로 실제 결제 상태를 확인해
     * <ul>
     *     <li>DONE(승인 완료) → 예약을 만들지 않았으므로 즉시 전액 취소하고 reversed=true 로 알린다.</li>
     *     <li>조회 결과 없음(404)·ABORTED·EXPIRED 등 → 과금되지 않았으므로 실패로만 알린다.</li>
     *     <li>조회/취소 자체가 실패 → 과금 여부를 확정할 수 없으므로 수동 확인 로그를 남긴다.</li>
     * </ul>
     */
    PaymentConfirmTimeoutException reconcileTimedOutConfirm(String orderId) {
        Optional<Map<String, Object>> payment;
        try {
            payment = findPaymentByOrderId(orderId);
        } catch (RuntimeException e) {
            log.error("[결제 상태 확인 실패] 승인 타임아웃 후 조회 실패, 수동 확인 필요: orderId={}, cause={}", orderId, e.toString());
            return new PaymentConfirmTimeoutException(
                    "결제 승인 결과를 확인하지 못했습니다. 결제 내역을 확인하시거나 고객센터로 문의해 주세요. (주문번호: " + orderId + ")",
                    false);
        }

        String status = payment.map(p -> (String) p.get("status")).orElse(null);
        if (!"DONE".equals(status)) {
            log.warn("승인 타임아웃 후 결제 상태 확인: 승인되지 않음 orderId={}, status={}", orderId, status);
            return new PaymentConfirmTimeoutException(
                    "결제 승인 응답이 지연되어 결제가 완료되지 않았습니다. 잠시 후 다시 시도해 주세요.", false);
        }

        String paymentKey = (String) payment.get().get("paymentKey");
        try {
            cancelPayment(paymentKey, "결제 승인 응답 시간 초과로 인한 자동 취소");
        } catch (RuntimeException cancelError) {
            log.error("[결제 보상 실패] 승인 타임아웃 후 자동 취소 실패, 수동 환불 필요: orderId={}, paymentKey={}, cause={}",
                    orderId, paymentKey, cancelError.toString());
            return new PaymentConfirmTimeoutException(
                    "결제 처리 중 오류가 발생했고 결제 자동 취소에도 실패했습니다. 고객센터로 문의해 주세요. (주문번호: " + orderId + ")",
                    false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        log.info("승인 타임아웃 결제 자동 취소 완료: orderId={}", orderId);
        return new PaymentConfirmTimeoutException(
                "결제 승인 응답이 지연되어 결제를 자동으로 취소했습니다. 다시 시도해 주세요.", true);
    }

    /** 주문번호로 결제 조회 (GET /v1/payments/orders/{orderId}). 결제가 없으면(404) empty. */
    Optional<Map<String, Object>> findPaymentByOrderId(String orderId) {
        return tossClient.findByOrderId(orderId);
    }

    public TossPaymentUrlDTO requestPayment(TossPaymentRequestDTO request) {
        Map<String, Object> body = Map.of(
                "amount", request.getAmount(),
                "orderId", request.getOrderId(),
                "orderName", request.getOrderName(),
                "customerName", request.getCustomerName(),
                "customerMobilePhone", request.getCustomerMobilePhone(),
                "successUrl", request.getSuccessUrl(),
                "failUrl", request.getFailUrl()
        );

        // body에는 고객 실명·전화번호가 포함되어 있어 통째로 로깅하지 않는다.
        log.info("Toss 결제 요청: orderId={}, amount={}", request.getOrderId(), request.getAmount());

        Map<String, Object> result = tossClient.createPayment(body);
        String paymentUrl = result != null ? (String) result.get("paymentUrl") : null;
        log.info("토스페이먼트 주소: {}", paymentUrl);

        return TossPaymentUrlDTO.builder()
                .paymentUrl(paymentUrl)
                .build();
    }

}
