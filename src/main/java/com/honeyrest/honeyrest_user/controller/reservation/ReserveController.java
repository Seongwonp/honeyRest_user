package com.honeyrest.honeyrest_user.controller.reservation;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationFormInfoDTO;
import com.honeyrest.honeyrest_user.dto.reservation.guest.GuestReservationLookupRequestDTO;
import com.honeyrest.domain.entity.Reservation;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.service.reservation.ReserveInfoService;
import com.honeyrest.honeyrest_user.service.reservation.ReserveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam Integer guests
    ) {
        // 비회원 예약 때문에 permitAll 이지만 userId 는 요청 파라미터가 아니라 인증 정보에서만 꺼낸다.
        // 비로그인이면 게스트로 취급한다.
        Long userId = (principal != null) ? principal.getUserId() : null;
        ReservationFormInfoDTO dto = reserveInfoService.getFormInfo(roomId, userId, checkIn, checkOut, guests);
        // dto에는 예약자 실명/전화번호/포인트/쿠폰이 포함되어 있어 통째로 로깅하지 않는다.
        log.info("예약폼 조회요청: roomId={}, checkIn={}, checkOut={}", roomId, checkIn, checkOut);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/guest-lookup")
    public ResponseEntity<ReservationCompleteDTO> guestLookup(@Valid @RequestBody GuestReservationLookupRequestDTO request) {
        ReservationCompleteDTO dto = reserveService.findGuestReservation(request);
        // dto에는 예약자 실명/전화번호 등 개인정보가 포함되어 있어 통째로 로깅하지 않는다.
        log.info("비회원 조회: reservationCode={}", request.getReservationCode());
        return ResponseEntity.ok(dto);
    }

}
