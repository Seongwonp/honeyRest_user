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
public class EventResponseDTO {
    private Long eventId;
    private String title;
    private String imageUrl;
    private String targetUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public static EventResponseDTO fromEntity(Event event) {
        return EventResponseDTO.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .imageUrl(event.getImageUrl())
                .targetUrl(event.getTargetUrl())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .build();
    }
}
