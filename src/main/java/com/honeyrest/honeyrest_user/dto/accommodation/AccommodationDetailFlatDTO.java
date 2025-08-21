package com.honeyrest.honeyrest_user.dto.accommodation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationDetailFlatDTO {
    private Long id;
    private String name;
    private String category;
    private String address;
    private BigDecimal price;
    private BigDecimal rating;                // 평균 평점
    private Integer reviewCount;                 // 리뷰 수
    private String intro;
    private String amenities;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String companyName;
    private String companyBusinessNumber;
    private Integer mainRegionId;
    private String mainRegionName;
    private Integer subRegionId;
    private String subRegionName;

}