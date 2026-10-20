package com.honeyrest.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true, nullable = false)
    private String tokenValue;

    private LocalDateTime expiryDate;

    private LocalDateTime createdAt;

    private Boolean isUsed;

    // 명시적 생성자 또는 빌더에서 직접 세팅
    public static PasswordResetToken create(User user, String tokenValue, LocalDateTime expiryDate) {
        return PasswordResetToken.builder()
                .user(user)
                .tokenValue(tokenValue)
                .expiryDate(expiryDate)
                .createdAt(LocalDateTime.now())
                .isUsed(false)
                .build();
    }

    /** createdAt·isUsed 가 비어 있을 때만 기본값을 채운다. */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isUsed == null) {
            this.isUsed = false;
        }
    }

}