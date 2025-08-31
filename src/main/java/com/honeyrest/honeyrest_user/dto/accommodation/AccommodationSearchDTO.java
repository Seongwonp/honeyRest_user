package com.honeyrest.honeyrest_user.dto.accommodation;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationSearchDTO {
    private Long id;                          // 숙소 ID
    private String title;                     // 숙소명
    private String location;                  // 주소 or 지역명
    private Double lat;
    private Double lng;
    private String image;                     // 썸네일 이미지 URL
    private BigDecimal price;                 // 할인된 가격
    private BigDecimal originalPrice;         // 할인 전 가격
    private BigDecimal rating;                // 평균 평점
    private Long reviewCount;                 // 리뷰 수
    private String category;                  // 숙소 유형
    private Boolean isWishlisted;             // 찜 여부

    private String mainRegionName;            // 상위 지역명 (예: 강원도)
    private String subRegionName;             // 하위 지역명 (예: 강릉시)
    private boolean available;
    private List<AccommodationTagMapDTO> tags;  // 태그 리스트 (예: ["오션뷰", "바베큐"])

    public void setTags(List<AccommodationTagMapDTO> tags) {
        this.tags = tags;
    }


    public AccommodationSearchDTO(Long id, String title, String location, String image,
                                  BigDecimal price, BigDecimal originalPrice, BigDecimal rating, Long reviewCount,
                                  String category, Boolean isWishlisted, String mainRegionName, String subRegionName,
                                  Double lat, Double lng) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.lat = lat;
        this.lng = lng;
        this.image = image;
        this.price = price;
        this.originalPrice = originalPrice;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.category = category;
        this.isWishlisted = isWishlisted;
        this.mainRegionName = mainRegionName;
        this.subRegionName = subRegionName;
    }
}