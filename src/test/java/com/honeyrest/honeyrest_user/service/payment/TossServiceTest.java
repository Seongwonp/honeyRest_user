package com.honeyrest.honeyrest_user.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.exception.PaymentConfirmTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("TossService 테스트")
class TossServiceTest {

    private MockRestServiceServer server;
    private TossService tossService;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        // 승인 → 조회 → 취소 순서를 검증하기 위해 순서 보장 모드(기본)를 사용한다.
        server = MockRestServiceServer.bindTo(restTemplate).build();
        tossService = new TossService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(tossService, "tossSecretKey", "test_sk");
    }

    private TossConfirmRequest confirmRequest() {
        return TossConfirmRequest.builder()
                .paymentKey("pk_1")
                .orderId("RES-1")
                .amount(BigDecimal.valueOf(1000))
                .reservationInfo(ReservationRequestDTO.builder().reservationCode("RES-1").build())
                .build();
    }

    @Test
    @DisplayName("승인 성공 응답을 결과 객체로 변환한다")
    void confirmSuccess() {
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic dGVzdF9zazo="))
                .andRespond(withSuccess("""
                        {"paymentKey":"pk_1","orderId":"RES-1","status":"DONE","method":"카드",
                         "totalAmount":1000,"receipt":{"url":"https://r"}}
                        """, MediaType.APPLICATION_JSON));

        TossPaymentResult result = tossService.confirmPayment(confirmRequest());

        assertThat(result.getStatus()).isEqualTo("DONE");
        assertThat(result.getAmount()).isEqualByComparingTo("1000");
        assertThat(result.getReceiptUrl()).isEqualTo("https://r");
        server.verify();
    }

    @Test
    @DisplayName("토스가 4xx 를 주면 토스 에러 메시지를 담은 ApiException 을 던진다")
    void confirmNon2xx() {
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"ALREADY_PROCESSED_PAYMENT\",\"message\":\"이미 처리된 결제 입니다.\"}"));

        assertThatThrownBy(() -> tossService.confirmPayment(confirmRequest()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("이미 처리된 결제 입니다.")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("결제 취소는 /v1/payments/{paymentKey}/cancel 로 cancelReason 을 보낸다")
    void cancel() {
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/pk_1/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.cancelReason").value("사유"))
                .andRespond(withSuccess("{\"orderId\":\"RES-1\",\"status\":\"CANCELED\"}", MediaType.APPLICATION_JSON));

        tossService.cancelPayment("pk_1", "사유");

        server.verify();
    }

    @Test
    @DisplayName("승인 타임아웃 후 조회 결과가 DONE 이면 결제를 취소하고 reversed=true 로 알린다")
    void confirmTimeout_doneIsCancelled() {
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/orders/RES-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Basic dGVzdF9zazo="))
                .andRespond(withSuccess("{\"paymentKey\":\"pk_1\",\"orderId\":\"RES-1\",\"status\":\"DONE\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/pk_1/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"orderId\":\"RES-1\",\"status\":\"CANCELED\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tossService.confirmPayment(confirmRequest()))
                .isInstanceOf(PaymentConfirmTimeoutException.class)
                .satisfies(e -> {
                    PaymentConfirmTimeoutException ex = (PaymentConfirmTimeoutException) e;
                    assertThat(ex.isReversed()).isTrue();
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                    assertThat(ex.getMessage()).contains("자동으로 취소");
                });
        server.verify();
    }

    @Test
    @DisplayName("승인 타임아웃 후 조회 결과가 없으면(404) 취소 없이 실패만 알린다")
    void confirmTimeout_notFoundIsFailureOnly() {
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/orders/RES-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"NOT_FOUND_PAYMENT\",\"message\":\"존재하지 않는 결제 입니다.\"}"));

        assertThatThrownBy(() -> tossService.confirmPayment(confirmRequest()))
                .isInstanceOf(PaymentConfirmTimeoutException.class)
                .satisfies(e -> {
                    PaymentConfirmTimeoutException ex = (PaymentConfirmTimeoutException) e;
                    assertThat(ex.isReversed()).isFalse();
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                });
        // 취소 요청은 기대 목록에 없으므로, 호출됐다면 verify 전에 이미 실패한다.
        server.verify();
    }

    @Test
    @DisplayName("승인 타임아웃 후 조회 결과가 ABORTED 면 취소 없이 실패만 알린다")
    void confirmTimeout_abortedIsFailureOnly() {
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/orders/RES-1"))
                .andRespond(withSuccess("{\"paymentKey\":\"pk_1\",\"orderId\":\"RES-1\",\"status\":\"ABORTED\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tossService.confirmPayment(confirmRequest()))
                .isInstanceOf(PaymentConfirmTimeoutException.class)
                .extracting(e -> ((PaymentConfirmTimeoutException) e).isReversed())
                .isEqualTo(false);
        server.verify();
    }
}
