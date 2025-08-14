package com.honeyrest.honeyrest_user.dto.user;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequestDTO {
    private String name;
    private String phone;
    private String birthDate;
    private String gender;
    private String profileImage;
    private Boolean marketingAgree;
}