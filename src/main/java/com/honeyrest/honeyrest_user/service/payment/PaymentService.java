package com.honeyrest.honeyrest_user.service.payment;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.entity.Payment;
import com.honeyrest.honeyrest_user.entity.Reservation;
import com.honeyrest.honeyrest_user.repository.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Log4j2
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment saveTossPayment(TossPaymentResult result, Reservation reservation) {
        Payment payment = Payment.builder()
                .userId(result.getReservationInfo().getUserId())
                .reservation(reservation)
                .amount(result.getAmount())
                .paymentMethod(result.getMethod())
                .paymentStatus(result.getStatus())
                .transactionId(result.getPaymentKey()) // 토스에서는 paymentKey가 거래 ID 역할
                .pgProvider("TOSS")
                .receiptUrl(result.getReceiptUrl())
                .paymentDate(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }

}