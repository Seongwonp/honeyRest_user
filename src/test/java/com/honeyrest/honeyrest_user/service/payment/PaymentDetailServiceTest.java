package com.honeyrest.honeyrest_user.service.payment;

import com.honeyrest.honeyrest_user.dto.payment.CardInfoDTO;
import com.honeyrest.honeyrest_user.dto.payment.VirtualAccountInfoDTO;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.entity.Payment;
import com.honeyrest.honeyrest_user.entity.PaymentDetail;
import com.honeyrest.honeyrest_user.repository.payment.PaymentDetailRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@Log4j2
@DisplayName("PaymentDetailService 테스트")
public class PaymentDetailServiceTest {

    @Mock
    private PaymentDetailRepository paymentDetailRepository;

    @InjectMocks
    private PaymentDetailService paymentDetailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("카드 결제 정보 저장")
    void testSaveTossDetails_CardPayment() {
        // Given: 카드 결제 정보가 포함된 토스 응답
        Map<String, Object> raw = new HashMap<>();
        raw.put("paymentKey", "toss_payment_key_123");
        raw.put("method", "CARD");
        raw.put("status", "DONE");

        Map<String, Object> cardInfo = new HashMap<>();
        cardInfo.put("issuerCode", "BC");
        cardInfo.put("number", "1234-****-****-5678");
        cardInfo.put("installmentPlanMonths", 3);
        raw.put("card", cardInfo);

        TossPaymentResult result = TossPaymentResult.builder()
                .paymentKey("toss_payment_key_123")
                .method("CARD")
                .amount(BigDecimal.valueOf(100000))
                .raw(raw)
                .build();

        Payment payment = Payment.builder()
                .paymentId(1L)
                .amount(BigDecimal.valueOf(100000))
                .paymentMethod("CARD")
                .paymentStatus("DONE")
                .pgProvider("TOSS")
                .build();

        // When: 결제 상세 정보 저장
        paymentDetailService.saveTossDetails(result, payment);

        // Then: Repository의 save 메서드가 호출되었는지 확인
        verify(paymentDetailRepository).save(any(PaymentDetail.class));
    }

    @Test
    @DisplayName("가상계좌 결제 정보 저장")
    void testSaveTossDetails_VirtualAccount() {
        // Given: 가상계좌 결제 정보가 포함된 토스 응답
        Map<String, Object> raw = new HashMap<>();
        raw.put("paymentKey", "toss_payment_key_456");
        raw.put("method", "VIRTUAL_ACCOUNT");
        raw.put("status", "WAITING_FOR_DEPOSIT");

        Map<String, Object> vaInfo = new HashMap<>();
        vaInfo.put("bank", "우리");
        vaInfo.put("accountNumber", "1002-123-456789");
        vaInfo.put("accountHolder", "허니레스트");
        vaInfo.put("dueDate", OffsetDateTime.now().plusDays(3).toString());
        raw.put("virtualAccount", vaInfo);

        TossPaymentResult result = TossPaymentResult.builder()
                .paymentKey("toss_payment_key_456")
                .method("VIRTUAL_ACCOUNT")
                .amount(BigDecimal.valueOf(50000))
                .raw(raw)
                .build();

        Payment payment = Payment.builder()
                .paymentId(2L)
                .amount(BigDecimal.valueOf(50000))
                .paymentMethod("VIRTUAL_ACCOUNT")
                .paymentStatus("WAITING_FOR_DEPOSIT")
                .pgProvider("TOSS")
                .build();

        // When: 결제 상세 정보 저장
        paymentDetailService.saveTossDetails(result, payment);

        // Then: Repository의 save 메서드가 호출되었는지 확인
        verify(paymentDetailRepository).save(any(PaymentDetail.class));
    }

    @Test
    @DisplayName("Payment가 null이면 예외 발생")
    void testSaveTossDetails_NullPayment() {
        // Given: Payment가 null
        TossPaymentResult result = TossPaymentResult.builder()
                .paymentKey("test_key")
                .raw(new HashMap<>())
                .build();

        // When & Then: IllegalArgumentException 발생
        assertThrows(IllegalArgumentException.class, () ->
                paymentDetailService.saveTossDetails(result, null)
        );
    }

    @Test
    @DisplayName("빈 카드/가상계좌 정보도 안전하게 처리")
    void testSaveTossDetails_EmptyPaymentInfo() {
        // Given: 카드/가상계좌 정보 없음
        Map<String, Object> raw = new HashMap<>();
        raw.put("paymentKey", "test_key");
        raw.put("method", "OTHER");

        TossPaymentResult result = TossPaymentResult.builder()
                .paymentKey("test_key")
                .method("OTHER")
                .amount(BigDecimal.valueOf(10000))
                .raw(raw)
                .build();

        Payment payment = Payment.builder()
                .paymentId(3L)
                .amount(BigDecimal.valueOf(10000))
                .paymentMethod("OTHER")
                .pgProvider("TOSS")
                .build();

        // When & Then: 예외 없이 정상 처리
        assertDoesNotThrow(() ->
                paymentDetailService.saveTossDetails(result, payment)
        );

        verify(paymentDetailRepository).save(any(PaymentDetail.class));
    }
}

