package com.honeyrest.honeyrest_user.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginRequestDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}