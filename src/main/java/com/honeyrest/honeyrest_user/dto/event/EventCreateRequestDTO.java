package com.honeyrest.honeyrest_user.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCreateRequestDTO {
    private String title;
    private String description;
    private String imageUrl;
    private String targetUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
}
