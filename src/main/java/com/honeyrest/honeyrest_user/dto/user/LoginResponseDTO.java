package com.honeyrest.honeyrest_user.dto.user;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    private UserResponseDTO user;
    private String accessToken;
}