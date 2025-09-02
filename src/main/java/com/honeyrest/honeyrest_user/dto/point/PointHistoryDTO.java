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
public class PointHistoryDTO {
    private Integer amount;
    private String type;
    private String reason;
    private Long relatedId;
    private Integer balance;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
