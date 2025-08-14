package com.honeyrest.honeyrest_user.dto.user;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private Long userId;
    private String email;
    private String name;
    private String phone;
    private String profileImage;
    private String role;
    private Boolean isVerified;
}