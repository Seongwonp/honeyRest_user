package com.honeyrest.honeyrest_user.controller.payment;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.service.payment.PaymentOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentOrchestrationService paymentOrchestrationService;

    @PostMapping("/toss/confirm")
    public ResponseEntity<ReservationCompleteDTO> confirmTossPayment(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody TossConfirmRequest request) throws Exception {
        if (principal == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        // reservationInfo.userId를 요청 바디 그대로 신뢰하면 인증된 사용자가 타인의 userId를 넣어
        // 그 사람의 쿠폰·포인트를 소모하고 예약을 그 사람 명의로 만들 수 있었다(P0-7). 항상 인증된
        // 사용자의 id로 덮어써서 이후 쿠폰/포인트 소유권 검증이 실제 로그인 사용자 기준으로 이뤄지게 한다.
        request.getReservationInfo().setUserId(principal.getUserId());
        ReservationCompleteDTO dto = paymentOrchestrationService.confirmAndSave(request);
        return ResponseEntity.ok(dto);
    }

}