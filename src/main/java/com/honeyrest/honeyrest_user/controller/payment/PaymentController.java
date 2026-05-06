package com.honeyrest.honeyrest_user.controller.payment;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.service.payment.PaymentOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ReservationCompleteDTO> confirmTossPayment(@Valid @RequestBody TossConfirmRequest request) throws Exception {
        ReservationCompleteDTO dto = paymentOrchestrationService.confirmAndSave(request);
        return ResponseEntity.ok(dto);
    }

}