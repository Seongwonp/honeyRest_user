package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.event.EventCreateRequestDTO;
import com.honeyrest.honeyrest_user.dto.event.EventResponseDTO;
import com.honeyrest.honeyrest_user.entity.Event;
import com.honeyrest.honeyrest_user.repository.EventRepository;
import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final FileUploadUtil fileUploadUtil;

    public List<EventResponseDTO> getActiveEvents() {
        LocalDateTime now = LocalDateTime.now();

        List<Event> events = eventRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(now, now);

        return events.stream()
                .map(EventResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }


    public Long createEvent(EventCreateRequestDTO dto, MultipartFile imageFile) throws Exception {
        String imageUrl = fileUploadUtil.upload(imageFile, "event");

        Event event = Event.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .imageUrl(imageUrl)
                .targetUrl(dto.getTargetUrl())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .isActive(dto.getIsActive())
                .build();

        eventRepository.save(event);
        return event.getEventId();
    }

}
