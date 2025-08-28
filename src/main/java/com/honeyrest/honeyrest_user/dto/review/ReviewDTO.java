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
public class ReviewDTO {
    private Long reviewId;
    private String nickname;
    private BigDecimal rating;
    private String content;
    private String reply;
    private Integer likeCount;
    private List<String> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}