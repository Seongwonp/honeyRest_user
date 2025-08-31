package com.honeyrest.honeyrest_user.controller.review;

import com.honeyrest.honeyrest_user.dto.review.ReviewRequestDTO;
import com.honeyrest.honeyrest_user.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/write")
    public ResponseEntity<Void> createReview(@RequestBody ReviewRequestDTO request) {
        reviewService.createReview(request);
        return ResponseEntity.ok().build();
    }

    // 좋아요 토글 API
    @PostMapping("/{reviewId}/like")
    public ResponseEntity<Integer> toggleLike(
            @PathVariable Long reviewId,
            @RequestParam boolean isLiked // true면 취소, false면 좋아요
    ) {
        int updatedCount = reviewService.toggleLike(reviewId, isLiked);
        return ResponseEntity.ok(updatedCount);
    }
}