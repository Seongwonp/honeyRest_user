package com.honeyrest.honeyrest_user.controller.review;

import com.honeyrest.honeyrest_user.dto.review.ReviewRequestDTO;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import com.honeyrest.honeyrest_user.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/write")
    public ResponseEntity<Void> createReview(@AuthenticationPrincipal CustomUserPrincipal principal,
                                             @Valid @RequestBody ReviewRequestDTO request) {
        reviewService.createReview(requireLogin(principal), request);
        return ResponseEntity.ok().build();
    }

    // 좋아요 토글 API
    // 사용자별 멱등: 같은 사용자가 여러 번 호출해도 좋아요는 1회만 반영된다.
    @PostMapping("/{reviewId}/like")
    public ResponseEntity<Integer> toggleLike(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long reviewId,
            @RequestParam boolean isLiked // true면 취소, false면 좋아요
    ) {
        int updatedCount = reviewService.toggleLike(requireLogin(principal), reviewId, isLiked);
        return ResponseEntity.ok(updatedCount);
    }

    private static Long requireLogin(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new ApiException("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }
        return principal.getUserId();
    }
}
