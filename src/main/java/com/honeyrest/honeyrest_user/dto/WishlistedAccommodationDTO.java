package com.honeyrest.honeyrest_user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistedAccommodationDTO {
    private Long id;
    private String name;
    private String address;
    private String thumbnail;
    private BigDecimal price;
    private BigDecimal rating;
    private String category;
    private String mainRegion;
    private String subRegion;
}