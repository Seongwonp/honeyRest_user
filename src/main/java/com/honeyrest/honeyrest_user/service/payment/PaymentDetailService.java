package com.honeyrest.honeyrest_user.service.payment;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.entity.Payment;
import com.honeyrest.honeyrest_user.entity.PaymentDetail;
import com.honeyrest.honeyrest_user.repository.payment.PaymentDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Map;

import static jakarta.xml.bind.DatatypeConverter.parseDateTime;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentDetailService {

    private final PaymentDetailRepository paymentDetailRepository;

    public void saveTossDetails(TossPaymentResult result, Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment 객체가 null입니다!");
        }

        Map<String, Object> raw = result.getRaw(); // 토스 응답 원본 Map

        // virtualAccount는 null일 수 있음 → 안전하게 처리
        Map<String, Object> virtualAccount = raw.get("virtualAccount") instanceof Map
                ? (Map<String, Object>) raw.get("virtualAccount")
                : Map.of();

        // 카드 정보는 raw 안에 존재하면 가져오기
        Map<String, Object> cardInfo = raw.get("card") instanceof Map
                ? (Map<String, Object>) raw.get("card")
                : Map.of();

        // 입금기한 날짜 파싱
        LocalDateTime expireDate = null;
        if (virtualAccount.get("dueDate") != null) {
            expireDate = convertToLocalDateTime((String) virtualAccount.get("dueDate"));
        }

        PaymentDetail detail = PaymentDetail.builder()
                .payment(payment) // Payment FK 연결
                .cardCompany((String) cardInfo.getOrDefault("issuerCode", null))
                .cardNumber((String) cardInfo.getOrDefault("number", null))
                .installmentMonths(cardInfo.get("installmentPlanMonths") instanceof Integer
                        ? (Integer) cardInfo.get("installmentPlanMonths")
                        : null)
                .virtualAccountNumber((String) virtualAccount.getOrDefault("accountNumber", null))
                .virtualAccountBank((String) virtualAccount.getOrDefault("bank", null))
                .virtualAccountHolder((String) virtualAccount.getOrDefault("accountHolder", null))
                .virtualAccountExpire(expireDate)
                .tossPaymentKey(result.getPaymentKey())
                .build();

        paymentDetailRepository.save(detail);
        log.info("💳 결제 상세 저장 완료: {}", detail);
    }

    // Calendar → LocalDateTime 변환 유틸
    private LocalDateTime convertToLocalDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return null;
        Calendar calendar = parseDateTime(dateTimeStr);
        return calendar.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}