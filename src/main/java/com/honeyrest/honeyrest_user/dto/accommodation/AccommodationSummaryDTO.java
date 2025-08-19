package com.honeyrest.honeyrest_user.dto.accommodation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationSummaryDTO {
    private Long id;
    private String title;
    private String location;
    private String image;
    private BigDecimal price;
    private BigDecimal rating;
    private String category;
}
