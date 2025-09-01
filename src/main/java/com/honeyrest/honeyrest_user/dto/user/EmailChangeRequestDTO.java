package com.honeyrest.honeyrest_user.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailChangeRequestDTO {
    private String newEmail;

    @JsonProperty("isPasswordVerified")
    private boolean passwordVerified;
}
