package com.honeyrest.honeyrest_user.dto.accommodation;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarAccommodationDTO {
    private Long id;
    private String name;
    private String thumbnail;
    private BigDecimal price;
    private BigDecimal rating;
}