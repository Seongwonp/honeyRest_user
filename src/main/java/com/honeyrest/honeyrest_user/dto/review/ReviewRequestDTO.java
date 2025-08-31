package com.honeyrest.honeyrest_user.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDTO {
    private Long reservationId;
    private BigDecimal rating;
    private BigDecimal cleanlinessRating;
    private BigDecimal serviceRating;
    private BigDecimal facilitiesRating;
    private BigDecimal locationRating;
    private String content;
    private List<String> imageUrls;
}
