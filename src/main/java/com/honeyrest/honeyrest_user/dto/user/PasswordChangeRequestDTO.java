package com.honeyrest.honeyrest_user.dto.user;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordChangeRequestDTO {
    private String currentPassword;
    private String newPassword;
}