package com.honeyrest.domain.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_coupon", indexes = {
        @Index(name = "idx_user_coupon_user_id", columnList = "user_id"),
        @Index(name = "idx_user_coupon_status", columnList = "status")
})
public class UserCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userCouponId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(nullable = false,length = 20)
    private String status; // 상태 (ISSUED, USED, EXPIRED)

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt; // 발급 일시

    @Column(name = "used_at")
    private LocalDateTime usedAt; // 사용 일시

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt; // 만료 일시

    /**
     * 메모리상의 상태만 USED 로 바꾼다.
     * 사용자 API 의 결제 경로는 동시 사용(이중 사용)을 막기 위해 이 메서드 대신
     * 조건부 UPDATE(status='ISSUED' 일 때만, UserCouponRepository.markUsedIfIssued)를 쓴다.
     */
    public void use() {
        this.status = "USED";
        this.usedAt = LocalDateTime.now();
    }

    /** 발급 상태(ISSUED)이고 만료 시각 전이면 true. */
    public boolean isIssuedAndUnexpired(LocalDateTime now) {
        return "ISSUED".equals(status) && (expiredAt == null || !expiredAt.isBefore(now));
    }

}

