package com.honeyrest.honeyrest_user.repository.payment;

import com.honeyrest.domain.entity.Payment;
import com.honeyrest.domain.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReservation(Reservation reservation);
    boolean existsByTransactionId(String transactionId);
}
