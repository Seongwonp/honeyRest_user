package com.honeyrest.honeyrest_user.service.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentRequestDTO;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentUrlDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class TossService {

    @Value("${com.tjfgusdh.toss.widgetSecretKey}")
    private String tossSecretKey;

    private static final String TOSS_PAYMENTS_API = "https://api.tosspayments.com/v1/payments";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 토스 결제 승인 (POST /v1/payments/confirm). 이 호출이 성공하면 실제 과금이 일어난다.
     * 토스가 2xx 가 아닌 응답을 주면 토스 에러 메시지를 담은 ApiException 을 던진다.
     */
    public TossPaymentResult confirmPayment(TossConfirmRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", request.getOrderId());
        body.put("amount", request.getAmount());
        body.put("paymentKey", request.getPaymentKey());

        Map<String, Object> responseMap = post(TOSS_PAYMENTS_API + "/confirm", body, "결제 승인");

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
        String url = UriComponentsBuilder.fromUriString(TOSS_PAYMENTS_API)
                .pathSegment(paymentKey, "cancel")
                .encode()
                .toUriString();
        Map<String, Object> responseMap = post(url, Map.of("cancelReason", cancelReason), "결제 취소");
        log.info("토스 결제 취소 완료: orderId={}, status={}", responseMap.get("orderId"), responseMap.get("status"));
    }

    /** 토스 API 공통 POST. 2xx 가 아니면 토스 에러 코드/메시지로 ApiException 을 던진다. */
    private Map<String, Object> post(String url, Object body, String action) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(tossSecretKey, "", StandardCharsets.UTF_8);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    URI.create(url), HttpMethod.POST, new HttpEntity<>(body, headers), MAP_TYPE);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new ApiException("토스 " + action + " 응답이 비어 있습니다.", HttpStatus.BAD_GATEWAY);
            }
            return responseBody;
        } catch (HttpStatusCodeException e) {
            String code = null;
            String message = null;
            try {
                Map<String, Object> error = objectMapper.readValue(e.getResponseBodyAsString(), new TypeReference<>() {});
                code = error.get("code") != null ? error.get("code").toString() : null;
                message = error.get("message") != null ? error.get("message").toString() : null;
            } catch (Exception parseError) {
                log.debug("토스 에러 응답 파싱 실패", parseError);
            }
            log.warn("토스 {} 실패: httpStatus={}, code={}, message={}", action, e.getStatusCode().value(), code, message);
            HttpStatus status = e.getStatusCode().is4xxClientError() ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
            throw new ApiException("토스 " + action + " 실패: " + (message != null ? message : "알 수 없는 오류"), status);
        } catch (ResourceAccessException e) {
            log.error("토스 {} 통신 오류: {}", action, e.getMessage());
            throw new ApiException("토스 결제 서버와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.GATEWAY_TIMEOUT);
        }
    }

    public TossPaymentUrlDTO requestPayment(TossPaymentRequestDTO request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(tossSecretKey, "");
        headers.setContentType(MediaType.APPLICATION_JSON);


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

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments", entity, Map.class
            );

            // 응답 바디에도 결제/구매자 정보가 포함될 수 있어 상태 코드만 남긴다.
            log.info("토스 응답 상태 코드: {}", response.getStatusCode());

            Map<String, Object> result = response.getBody();
            String paymentUrl = (String) result.get("paymentUrl");

            log.info("토스페이먼트 주소: {}", paymentUrl);

            return TossPaymentUrlDTO.builder()
                    .paymentUrl(paymentUrl)
                    .build();

        } catch (HttpServerErrorException e) {
            log.error("❌ 토스 API 서버 오류 발생");
            log.error("상태 코드: {}", e.getStatusCode());
            log.error("응답 바디: {}", e.getResponseBodyAsString());
            throw new IllegalStateException("토스 결제 요청 실패: " + e.getMessage());
        }
    }

}
