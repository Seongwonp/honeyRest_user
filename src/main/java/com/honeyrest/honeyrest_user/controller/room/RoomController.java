package com.honeyrest.honeyrest_user.controller.room;

import com.honeyrest.honeyrest_user.dto.room.RoomDetailDTO;
import com.honeyrest.honeyrest_user.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room")
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDetailDTO> getRoomDetail(
            @PathVariable Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Integer guests
    ) {
        log.info("[Controller] Room 상세 요청: roomId={}, checkIn={}, checkOut={}, guests={}", roomId, checkIn, checkOut, guests);
        RoomDetailDTO dto = roomService.getRoomDetail(roomId, checkIn, checkOut, guests);
        return ResponseEntity.ok(dto);
    }
}