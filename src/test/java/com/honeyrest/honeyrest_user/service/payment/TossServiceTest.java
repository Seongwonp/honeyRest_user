package com.honeyrest.honeyrest_user.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.exception.ApiException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("TossService 테스트")
class TossServiceTest {

    private MockRestServiceServer server;
    private TossService tossService;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
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
}
