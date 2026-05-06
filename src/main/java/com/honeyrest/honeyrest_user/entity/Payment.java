package com.honeyrest.honeyrest_user.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_reservation_id", columnList = "reservation_id"),
        @Index(name = "idx_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_transaction_id", columnList = "transaction_id")
})
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId; // 결제 고유 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 30, nullable = false)
    private String paymentMethod;

    @Column(name = "payment_status", length = 20, nullable = false)
    private String paymentStatus;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "pg_provider", length = 50)
    private String pgProvider; // pg사 이름

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl; // 영수증 url

    @Column(name = "payment_date")
    private LocalDateTime paymentDate; // 결제일시

    // ===== 양방향 관계 =====
    @OneToOne(mappedBy = "payment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private PaymentDetail paymentDetail;

    // ===== 편의 메서드 =====

    /**
     * 결제 완료 상태 확인
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(paymentStatus) || "DONE".equals(paymentStatus);
    }

    /**
     * 결제 환불 상태 확인
     */
    public boolean isCancelled() {
        return "CANCELLED".equals(paymentStatus) || "REFUNDED".equals(paymentStatus);
    }

    /**
     * 토스 결제 여부 확인
     */
    public boolean isTossPayment() {
        return "TOSS".equals(pgProvider);
    }

}
