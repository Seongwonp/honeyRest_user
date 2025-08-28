package com.honeyrest.honeyrest_user.controller.user;

import com.honeyrest.honeyrest_user.dto.WishToggleRequestDTO;

import com.honeyrest.honeyrest_user.service.WishListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishList")
public class WishListController {

    private final WishListService wishListService;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleWish(@RequestBody WishToggleRequestDTO dto) {
        log.info("🧡 찜 토글 요청: userId={}, accommodationId={}", dto.getUserId(), dto.getAccommodationId());

        boolean liked = wishListService.toggleWish(dto.getUserId(), dto.getAccommodationId());
        return ResponseEntity.ok(Map.of("liked", liked));
    }
}