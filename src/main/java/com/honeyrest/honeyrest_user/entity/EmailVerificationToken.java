package com.honeyrest.honeyrest_user.entity;

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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.expiryDate = createdAt.plusHours(24);
    }
}