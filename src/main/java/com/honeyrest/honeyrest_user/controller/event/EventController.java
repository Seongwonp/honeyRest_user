package com.honeyrest.honeyrest_user.controller.event;

import com.honeyrest.honeyrest_user.dto.event.EventCreateRequestDTO;
import com.honeyrest.honeyrest_user.dto.event.EventResponseDTO;
import com.honeyrest.honeyrest_user.entity.Event;
import com.honeyrest.honeyrest_user.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/activeList")
    public ResponseEntity<List<EventResponseDTO>> getActiveEvents() {
        List<EventResponseDTO> events = eventService.getActiveEvents();
        return ResponseEntity.ok(events);
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createEvent(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String targetUrl,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Boolean isActive,
            @RequestParam MultipartFile imageFile
    )throws Exception {
        // 날짜 파싱
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        // DTO 생성
        EventCreateRequestDTO dto = EventCreateRequestDTO.builder()
                .title(title)
                .description(description)
                .targetUrl(targetUrl)
                .startDate(start)
                .endDate(end)
                .isActive(isActive)
                .build();

        // 서비스 호출
        Long eventId = eventService.createEvent(dto, imageFile);

        return ResponseEntity.ok(eventId);
    }

}