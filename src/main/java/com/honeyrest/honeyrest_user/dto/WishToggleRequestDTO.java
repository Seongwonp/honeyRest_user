package com.honeyrest.honeyrest_user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WishToggleRequestDTO {
    private Long userId;
    private Long accommodationId;
}
