package com.honeyrest.honeyrest_user.service.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.exception.ApiException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * 실제 토스페이먼츠 API 를 호출하는 {@link TossClient} (e2e 프로필을 제외한 모든 프로필).
 * 시크릿 키는 Basic 인증(시크릿키:빈 비밀번호)으로 보낸다.
 */
@Log4j2
@Component
@Profile("!e2e")
public class HttpTossClient implements TossClient {

    static final String TOSS_PAYMENTS_API = "https://api.tosspayments.com/v1/payments";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String tossSecretKey;

    public HttpTossClient(RestTemplate restTemplate,
                          ObjectMapper objectMapper,
                          @Value("${com.tjfgusdh.toss.widgetSecretKey}") String tossSecretKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.tossSecretKey = tossSecretKey;
    }

    @Override
    public Map<String, Object> confirm(Map<String, Object> body) {
        return post(TOSS_PAYMENTS_API + "/confirm", body, "결제 승인");
    }

    @Override
    public Map<String, Object> cancel(String paymentKey, String cancelReason) {
        String url = UriComponentsBuilder.fromUriString(TOSS_PAYMENTS_API)
                .pathSegment(paymentKey, "cancel")
                .encode()
                .toUriString();
        return post(url, Map.of("cancelReason", cancelReason), "결제 취소");
    }

    @Override
    public Optional<Map<String, Object>> findByOrderId(String orderId) {
        String url = UriComponentsBuilder.fromUriString(TOSS_PAYMENTS_API)
                .pathSegment("orders", orderId)
                .encode()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(tossSecretKey, "", StandardCharsets.UTF_8);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    URI.create(url), HttpMethod.GET, new HttpEntity<>(headers), MAP_TYPE);
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> createPayment(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(tossSecretKey, "");
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    TOSS_PAYMENTS_API, new HttpEntity<>(body, headers), Map.class);
            // 응답 바디에는 결제/구매자 정보가 포함될 수 있어 상태 코드만 남긴다.
            log.info("토스 응답 상태 코드: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpServerErrorException e) {
            log.error("❌ 토스 API 서버 오류 발생");
            log.error("상태 코드: {}", e.getStatusCode());
            log.error("응답 바디: {}", e.getResponseBodyAsString());
            throw new IllegalStateException("토스 결제 요청 실패: " + e.getMessage());
        }
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
}
