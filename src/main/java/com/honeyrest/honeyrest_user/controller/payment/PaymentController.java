package com.honeyrest.honeyrest_user.controller.payment;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.service.payment.PaymentOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import com.honeyrest.honeyrest_user.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            // 미인증은 403(AccessDenied)이 아니라 401 이어야 프론트가 토큰 재발급/로그인 흐름을 탄다.
            throw new ApiException("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }
        // 요청 바디의 userId 는 신뢰하지 않고 인증된 사용자 id 로 덮어쓴다 (쿠폰·포인트 소유권 검증 기준).
        request.getReservationInfo().setUserId(principal.getUserId());
        ReservationCompleteDTO dto = paymentOrchestrationService.confirmAndSave(request);
        return ResponseEntity.ok(dto);
    }

}