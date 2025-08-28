package com.honeyrest.honeyrest_user.controller.user;

import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.service.reservation.ReserveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final ReserveService reserveService;

    @GetMapping("/reservations")
    public ResponseEntity<PageResponseDTO<ReservationCompleteDTO>> getMyReservations(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 5, sort = "checkInDate", direction = DESC) Pageable pageable) {

        Long userId = principal.getUserId(); // 토큰에서 추출된 인증된 사용자 ID
        log.info("📋 예약 조회 요청: userId={}", userId);

        return ResponseEntity.ok(reserveService.getUserReservations(userId, pageable));
    }
}
