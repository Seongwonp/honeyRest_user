package com.honeyrest.honeyrest_user.dto.user;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialLoginRequestDTO {
    private String socialType; // KAKAO, GOOGLE 등
    private String socialId;
    private String email;
    private String name;
    private String profileImage;
}