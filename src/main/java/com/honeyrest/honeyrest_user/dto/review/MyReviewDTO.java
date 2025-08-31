package com.honeyrest.honeyrest_user.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyReviewDTO {
    private Long reviewId;
    private List<String> imageUrls;
    private String accommodationName;
    private String roomName;
    private String reply;
    private BigDecimal rating;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
