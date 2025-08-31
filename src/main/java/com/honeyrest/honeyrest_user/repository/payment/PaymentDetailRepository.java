package com.honeyrest.honeyrest_user.repository.payment;

import com.honeyrest.honeyrest_user.entity.Payment;
import com.honeyrest.honeyrest_user.entity.PaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentDetailRepository extends JpaRepository<PaymentDetail, Long> {

    Optional<PaymentDetail> findByPayment(Payment payment);
}
