package com.honeyrest.honeyrest_user.controller.user;

import com.honeyrest.honeyrest_user.dto.WishToggleRequestDTO;

import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.service.WishListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishList")
public class WishListController {

    private final WishListService wishListService;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleWish(@AuthenticationPrincipal CustomUserPrincipal principal,
                                        @RequestBody WishToggleRequestDTO dto) {
        // 요청 바디의 userId를 신뢰하면 타인의 찜 목록을 임의로 추가/삭제할 수 있었다(P1-4).
        Long userId = principal.getUserId();
        log.info("🧡 찜 토글 요청: userId={}, accommodationId={}", userId, dto.getAccommodationId());

        boolean liked = wishListService.toggleWish(userId, dto.getAccommodationId());
        return ResponseEntity.ok(Map.of("liked", liked));
    }
}