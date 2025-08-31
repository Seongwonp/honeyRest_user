package com.honeyrest.honeyrest_user.dto.user;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileUpdateRequestDTO {
    private String name;
    private String phone;
    private String email;
}