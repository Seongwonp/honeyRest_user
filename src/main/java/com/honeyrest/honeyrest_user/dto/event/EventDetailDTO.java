package com.honeyrest.honeyrest_user.dto.event;

import com.honeyrest.honeyrest_user.entity.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetailDTO {
    private Long eventId;
    private String title;
    private String description;
    private String imageUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String targetUrl;
    private Boolean isActive;

    public static EventDetailDTO from(Event event) {
        return EventDetailDTO.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .description(event.getDescription())
                .imageUrl(event.getImageUrl())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .targetUrl(event.getTargetUrl())
                .isActive(event.getIsActive())
                .build();
    }
}