package com.honeyrest.honeyrest_user.service.payment;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentRequestDTO;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentUrlDTO;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.entity.Coupon;
import com.honeyrest.honeyrest_user.repository.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class TossService {

    @Value("${com.tjfgusdh.toss.widgetSecretKey}")
    private String tossSecretKey;

    private final RestTemplate restTemplate;
    private final CouponRepository couponRepository;

    public TossPaymentResult confirmPayment(TossConfirmRequest request) throws Exception {
        // 요청 JSON 구성
        JSONObject obj = new JSONObject();
        obj.put("orderId", request.getOrderId());
        obj.put("amount", request.getAmount());
        obj.put("paymentKey", request.getPaymentKey());

        // 인증 헤더 구성
        String authorization = "Basic " + Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        // API 호출
        URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", authorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(obj.toString().getBytes(StandardCharsets.UTF_8));

        int code = connection.getResponseCode();
        InputStream responseStream = code == 200 ? connection.getInputStream() : connection.getErrorStream();

        // Jackson으로 응답 파싱
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
        Map<String, Object> responseMap = mapper.readValue(responseStream, typeRef);
        responseStream.close();

        log.info("✅ 토스 결제 승인 응답 수신: orderId={}, status={}",
                responseMap.get("orderId"), responseMap.get("status"));

        //  안전한 필드 추출
        BigDecimal amount = responseMap.get("totalAmount") != null
                ? new BigDecimal(responseMap.get("totalAmount").toString())
                : BigDecimal.ZERO;

        String receiptUrl = responseMap.get("receiptUrl") != null
                ? responseMap.get("receiptUrl").toString()
                : null;


        TossPaymentResult result = TossPaymentResult.builder()
                .paymentKey((String) responseMap.get("paymentKey"))
                .orderId((String) responseMap.get("orderId"))
                .method((String) responseMap.get("method"))
                .status((String) responseMap.get("status"))
                .amount(amount)
                .receiptUrl(receiptUrl)
                .reservationInfo(request.getReservationInfo())   // 프론트에서 받은 예약정보 유지
                .couponName(request.getReservationInfo().getCouponId() != null ? "쿠폰적용" : null) // 필요하면 추가
                .isEmailSend(request.getReservationInfo().getIsEmailSend())
                .raw(responseMap) // 토스 원본 JSON도 저장하고 싶으면
                .build();
        log.info("🎯 최종 TossPaymentResult: {}", result);
        return result;
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

        log.info("Toss 결제 요청: {}", body);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments", entity, Map.class
            );

            log.warn("토스 응답 상태 코드: {}", response.getStatusCode());
            log.warn("토스 응답 바디: {}", response.getBody());

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
