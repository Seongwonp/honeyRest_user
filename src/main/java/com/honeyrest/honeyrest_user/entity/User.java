package com.honeyrest.honeyrest_user.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId; //사용자 고유 식별자

    @Column(nullable = false, length = 100, unique = true)
    private String email; // 이메일 주소 (자체 가입용)

    @Column(name = "password_hash", length = 255)
    private String passwordHash; // 비밀번호 해시 (자체 가입용)

    @Column(name = "social_type", length = 20)
    private String socialType; // 소셜 로그인 타입 (KAKAO, GOOGLE)

    @Column(name = "social_id", length = 100)
    private String socialId; // 소셜 서비스 고유 ID

    @Column(nullable = false, length = 50)
    private String name; // 사용자 이름

    @Column(length = 20)
    private String phone; // 연락처

    @Column(name = "profile_image", length = 500)
    private String profileImage; // 프로필 이미지 URL

    @Column(name = "birth_date")
    private LocalDate birthDate; // 생년월일

    @Column(length = 10)
    private String gender; // 성별

    @Column(name = "marketing_agree")
    private Boolean marketingAgree; // 마케팅 정보 수신 동의 여부

    private int point; // 현재 포인트

    @Column(length = 20)
    private String role; // 권한 (GENERAL, COMPANY_ADMIN, SUPER_ADMIN)

    @Column(length = 20)
    private String status; // 계정 상태 (ACTIVE, SUSPENDED, DELETED)

    @Column(name = "last_login")
    private LocalDateTime lastLogin; // 마지막 로그인 시간

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false; // 이메일 인증 여부

    @Column(name = "token_valid_after")
    private LocalDateTime tokenValidAfter; // 이 시각보다 먼저 발급된 access token은 모두 무효로 취급한다

    public void verify() {
        this.isVerified = true;
    }
    public void updateLastLogin(LocalDateTime time) {
        this.lastLogin = time;
    }
    public void updateEmail(String newEmail) {
        this.email = newEmail;
    }
    public void updateProfile(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
    public void updateProfileImage(String imageUrl) {
        this.profileImage = imageUrl;
    }

    public void setPassword(String encode) {
        this.passwordHash = encode;
    }

    public void updatePoint(int newPoint) {
        this.point = newPoint;
    }
    public void deleteAccount() {
        this.status = "DELETED";
    }

    /**
     * 로그아웃·비밀번호 변경 시 호출: 그 이전에 발급된 access token을 전부 무효화한다.
     * <p>
     * JWT {@code iat}는 초 단위라서 이 값도 초 단위로 맞춘다. 현재 시각을 초 단위로 내린 뒤 1초를 더해
     * "폐기 시점과 같은 초에 이미 발급된 토큰"까지 확실히 무효화한다(iat &lt; tokenValidAfter → 거부).
     * 폐기 직후 재로그인으로 발급되는 토큰은 {@code JwtTokenProvider.createAccessToken(User)}가
     * iat = max(현재 시각, tokenValidAfter)로 발급하므로 같은 초 안에 재로그인해도 정상 인증된다.
     */
    public void revokeExistingTokens() {
        this.tokenValidAfter = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
    }

}
