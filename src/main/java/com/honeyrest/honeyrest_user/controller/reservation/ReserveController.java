package com.honeyrest.honeyrest_user.controller.reservation;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationFormInfoDTO;
import com.honeyrest.honeyrest_user.dto.reservation.guest.GuestReservationLookupRequestDTO;
import com.honeyrest.honeyrest_user.entity.Reservation;
import com.honeyrest.honeyrest_user.service.reservation.ReserveInfoService;
import com.honeyrest.honeyrest_user.service.reservation.ReserveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reserve")
public class ReserveController {

    private final ReserveService reserveService;
    private final ReserveInfoService reserveInfoService;

    @GetMapping("/form-info")
    public ResponseEntity<ReservationFormInfoDTO> getReservationFormInfo(
            @RequestParam Long roomId,
            @RequestParam(required = false) Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam Integer guests
    ) {
        ReservationFormInfoDTO dto = reserveInfoService.getFormInfo(roomId, userId, checkIn, checkOut, guests);
        log.info("예약폼 조회요청: {}", dto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/guest-lookup")
    public ResponseEntity<ReservationCompleteDTO> guestLookup(@RequestBody GuestReservationLookupRequestDTO request) {
        ReservationCompleteDTO dto = reserveService.findGuestReservation(request);
        log.info("비회원 조회: {}",dto);
        return ResponseEntity.ok(dto);
    }

}
