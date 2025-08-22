package com.honeyrest.honeyrest_user.dto.accommodation;

import com.honeyrest.honeyrest_user.dto.CancellationPolicyDTO;
import com.honeyrest.honeyrest_user.dto.company.CompanyDTO;
import com.honeyrest.honeyrest_user.dto.location.LocationDTO;
import com.honeyrest.honeyrest_user.dto.region.RegionDTO;
import com.honeyrest.honeyrest_user.dto.review.ReviewDTO;
import com.honeyrest.honeyrest_user.dto.room.RoomDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationDetailDTO {
    private Long id;
    private String name;
    private String category;
    private String address;
    private BigDecimal price;
    private BigDecimal rating;                // 평균 평점
    private Integer reviewCount;                 // 리뷰 수
    private boolean isWished;
    private List<String> images;
    private List<AccommodationTagDTO> tags;
    private List<RoomDTO> rooms;
    private String intro;
    private List<String> facilities;
    private String usage;
    private CompanyDTO company;
    private RegionDTO mainRegion;
    private RegionDTO subRegion;
    private LocationDTO location;
    private List<ReviewDTO> reviews;
    private List<CancellationPolicyDTO> cancellationPolicies;
    private List<SimilarAccommodationDTO> similar;
    private LocalDate checkIn;
    private LocalDate checkOut;
}