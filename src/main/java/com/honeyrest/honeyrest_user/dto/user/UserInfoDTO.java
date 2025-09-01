package com.honeyrest.honeyrest_user.dto.user;

import com.honeyrest.honeyrest_user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String profileImage;
    private String provider; // 소셜 타입 or local
    private String role;
    private String status;

    public static UserInfoDTO from(User user) {
        return UserInfoDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImage(user.getProfileImage())
                .provider(user.getSocialType() != null ? user.getSocialType() : "local")
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
