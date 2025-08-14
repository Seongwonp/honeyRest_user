package com.honeyrest.honeyrest_user.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenStatusResponseDTO {
    private boolean exists;
    private boolean isVerified;
    private boolean isExpired;
}
