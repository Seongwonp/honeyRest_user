package com.honeyrest.honeyrest_user.dto.point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PointHistoryRequestDTO {
    private Long userId;
    private Integer amount;
    private String type; // "SAVE", "USE", "EXPIRE"
    private String reason;
    private Long relatedId;
    private LocalDateTime expiresAt;
}
