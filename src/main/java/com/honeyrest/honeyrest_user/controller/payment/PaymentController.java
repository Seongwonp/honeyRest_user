package com.honeyrest.honeyrest_user.controller.payment;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;

import com.honeyrest.honeyrest_user.entity.Payment;
import com.honeyrest.honeyrest_user.entity.Reservation;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.mapper.ReservationMapper;
import com.honeyrest.honeyrest_user.service.email.EmailService;
import com.honeyrest.honeyrest_user.service.payment.PaymentDetailService;
import com.honeyrest.honeyrest_user.service.payment.PaymentService;
import com.honeyrest.honeyrest_user.service.payment.TossService;
import com.honeyrest.honeyrest_user.service.reservation.ReserveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentDetailService paymentDetailService;
    private final TossService tossService;
    private final ReserveService reservationService;
    private final ReservationMapper reservationMapper;
    private final EmailService emailService;

    @PostMapping("/toss/confirm")
    public ResponseEntity<ReservationCompleteDTO> confirmTossPayment(@RequestBody TossConfirmRequest request) throws Exception {

        // 1️⃣ 토스 결제 승인
        TossPaymentResult result = tossService.confirmPayment(request);
        log.info("🎯 최종 TossPaymentResult: {}", result);

        // 2️⃣ 결제 실패 처리
        if ("FAILED".equals(result.getStatus())) {
            log.warn("❌ 결제 승인 실패: {}", result.getRaw());
            return ResponseEntity.status(400).body(null);
        }

        // 3️⃣ 예약 먼저 저장
        Reservation reservation = reservationService.createReservation(result.getReservationInfo(), result.getAmount(),result.getReservationInfo().getDiscountAmount());
        log.info("예약 등록 완료: {}", reservation);

        // 4️⃣ 결제 정보 저장 (예약 ID FK 연결)
        Payment payment = paymentService.saveTossPayment(result, reservation);
        if (payment == null) {
            throw new IllegalStateException("Payment 저장 실패!");
        }
        log.info("결제 정보 저장 완료: {}", payment);

        // 5️⃣ 결제 상세 정보 저장
        paymentDetailService.saveTossDetails(result, payment);
        log.info("결제 상세 정보 저장 완료");

        // 6️⃣ 최종 응답 DTO 생성
        ReservationCompleteDTO dto = reservationMapper.toCompleteDTO(reservation, payment);
        dto.setCouponName(result.getCouponName());
        dto.setIsEmailSent(result.getIsEmailSend());
        log.info("최종 응답 DTO 생성 완료: {}", dto);

        // 7️⃣ 이메일 발송 (조건부)
        if (Boolean.TRUE.equals(result.getIsEmailSend())) {
            User user = reservation.getUser();
            emailService.sendReservationConfirmation(user, dto);
            log.info("📧 예약 완료 이메일 발송 완료: {}", user.getEmail());
            dto.setIsEmailSent(true);
        } else {
            log.info("📧 이메일 발송 생략됨 (사용자 동의 없음)");
            dto.setIsEmailSent(false);
        }

        return ResponseEntity.ok(dto);
    }

}