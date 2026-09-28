package com.honeyrest.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_token", indexes = {
        @Index(name = "idx_email_verification_token_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true, nullable = false)
    private String tokenValue;

    private String tokenType; // 예: SIGNUP, EMAIL_CHANGE

    @Column(name = "pending_email")
    private String pendingEmail; // EMAIL_CHANGE 토큰이 승인하는 새 이메일. 확정 시 파라미터와 대조한다.

    private LocalDateTime expiryDate;

    private LocalDateTime createdAt;

    private Boolean isVerified;

    /**
     * [통합] 사용자 사본: createdAt/expiryDate(+24h) 설정, 호스트 사본: createdAt/isVerified=false 설정.
     * 두 동작을 합쳤다. is_verified 는 NOT NULL 이므로 빌더에서 비워 둔 경우에만 false 로 채운다.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.expiryDate = createdAt.plusHours(24);
        if (this.isVerified == null) {
            this.isVerified = false;
        }
    }
}