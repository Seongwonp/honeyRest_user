package com.honeyrest.honeyrest_user.e2e;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.service.payment.TossService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("E2eTossClient(토스 스텁) 테스트")
class E2eTossClientTest {

    private final E2eTossClient stub = new E2eTossClient();
    private final TossService tossService = new TossService(stub);

    @Test
    @DisplayName("어떤 결제든 DONE 으로 승인하고 요청 금액을 그대로 돌려준다 (TossService 경유)")
    void confirmApprovesAnything() {
        TossPaymentResult result = tossService.confirmPayment(TossConfirmRequest.builder()
                .paymentKey("e2e_pk")
                .orderId("HR-E2E00001")
                .amount(BigDecimal.valueOf(200000))
                .reservationInfo(ReservationRequestDTO.builder().reservationCode("HR-E2E00001").build())
                .build());

        assertThat(result.getStatus()).isEqualTo("DONE");
        assertThat(result.getPaymentKey()).isEqualTo("e2e_pk");
        assertThat(result.getOrderId()).isEqualTo("HR-E2E00001");
        assertThat(result.getAmount()).isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("승인한 주문은 조회 시 DONE, 모르는 주문은 없음")
    void findByOrderId() {
        stub.confirm(Map.of("paymentKey", "pk", "orderId", "HR-1", "amount", 1000));

        assertThat(stub.findByOrderId("HR-1")).get().extracting(p -> p.get("status")).isEqualTo("DONE");
        assertThat(stub.findByOrderId("HR-unknown")).isEmpty();
    }

    @Test
    @DisplayName("취소 요청을 기록한다")
    void recordsCancellations() {
        stub.confirm(Map.of("paymentKey", "pk", "orderId", "HR-1", "amount", 1000));

        tossService.cancelPayment("pk", "예약 저장 실패로 인한 자동 취소");

        assertThat(stub.cancellations()).singleElement()
                .satisfies(c -> {
                    assertThat(c.paymentKey()).isEqualTo("pk");
                    assertThat(c.cancelReason()).isEqualTo("예약 저장 실패로 인한 자동 취소");
                });
    }
}
