package com.honeyrest.honeyrest_user.service.payment;

import com.honeyrest.honeyrest_user.dto.payment.CardInfoDTO;
import com.honeyrest.honeyrest_user.dto.payment.VirtualAccountInfoDTO;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.entity.Payment;
import com.honeyrest.honeyrest_user.entity.PaymentDetail;
import com.honeyrest.honeyrest_user.repository.payment.PaymentDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentDetailService {

    private final PaymentDetailRepository paymentDetailRepository;

    /**
     * 토스 결제 응답에서 결제 상세 정보를 추출하여 저장
     * @param result 토스 결제 응답 결과
     * @param payment 저장할 Payment 엔티티
     */
    public void saveTossDetails(TossPaymentResult result, Payment payment) {
        validatePayment(payment);

        Map<String, Object> raw = result.getRaw();

        // 구조화된 정보 추출
        CardInfoDTO cardInfo = extractCardInfo(raw);
        VirtualAccountInfoDTO virtualAccountInfo = extractVirtualAccountInfo(raw);

        // PaymentDetail 엔티티 구성
        PaymentDetail detail = PaymentDetail.builder()
                .payment(payment)
                .cardCompany(cardInfo.getCompany())
                .cardNumber(cardInfo.getNumber())
                .installmentMonths(cardInfo.getInstallmentMonths())
                .virtualAccountNumber(virtualAccountInfo.getAccountNumber())
                .virtualAccountBank(virtualAccountInfo.getBank())
                .virtualAccountHolder(virtualAccountInfo.getAccountHolder())
                .virtualAccountExpire(virtualAccountInfo.getDueDate())
                .tossPaymentKey(result.getPaymentKey())
                .build();

        paymentDetailRepository.save(detail);
        log.info("💳 결제 상세 저장 완료: paymentId={}, method={}",
                payment.getPaymentId(), result.getMethod());
    }

    /**
     * 토스 응답 Map에서 카드 정보 추출
     */
    private CardInfoDTO extractCardInfo(Map<String, Object> raw) {
        Map<String, Object> cardMap = extractMapSafely(raw, "card");

        return CardInfoDTO.builder()
                .company(extractString(cardMap, "issuerCode"))
                .number(extractString(cardMap, "number"))
                .installmentMonths(extractInteger(cardMap, "installmentPlanMonths"))
                .build();
    }

    /**
     * 토스 응답 Map에서 가상계좌 정보 추출
     */
    private VirtualAccountInfoDTO extractVirtualAccountInfo(Map<String, Object> raw) {
        Map<String, Object> vaMap = extractMapSafely(raw, "virtualAccount");
        LocalDateTime dueDate = null;

        Object dueDateObj = vaMap.get("dueDate");
        if (dueDateObj != null) {
            dueDate = convertToLocalDateTime((String) dueDateObj);
        }

        return VirtualAccountInfoDTO.builder()
                .accountNumber(extractString(vaMap, "accountNumber"))
                .bank(extractString(vaMap, "bank"))
                .accountHolder(extractString(vaMap, "accountHolder"))
                .dueDate(dueDate)
                .build();
    }

    /**
     * Map에서 값을 안전하게 추출 (타입 체크 포함)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMapSafely(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Map.of();
    }

    /**
     * Map에서 String 값을 안전하게 추출
     */
    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : null;
    }

    /**
     * Map에서 Integer 값을 안전하게 추출
     */
    private Integer extractInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * Toss ISO-8601 날짜 문자열을 LocalDateTime으로 변환
     */
    private LocalDateTime convertToLocalDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(dateTimeStr).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.warn("⚠️ ISO-8601 파싱 실패, LocalDateTime으로 재시도: {}", dateTimeStr);
            try {
                return LocalDateTime.parse(dateTimeStr);
            } catch (DateTimeParseException ex) {
                log.error("❌ 날짜 파싱 실패: {}", dateTimeStr, ex);
                return null;
            }
        }
    }

    /**
     * Payment 객체 유효성 검증
     */
    private void validatePayment(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment 객체가 null입니다!");
        }
    }
}