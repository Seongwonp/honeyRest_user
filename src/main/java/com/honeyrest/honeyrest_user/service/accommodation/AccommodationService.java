package com.honeyrest.honeyrest_user.service.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationDetailDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSearchDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSummaryDTO;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.entity.Accommodation;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationRepository;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationDetail.AccommodationDetailQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final AccommodationDetailQueryRepository detailQueryRepository;

    /**
     * 인기 숙소 조회 (카테고리별)
     */
    @Cacheable(value = "popularAccommodations", key = "#category", unless = "#result == null || #result.isEmpty()")
    public List<AccommodationSummaryDTO> getPopularByCategory(String category) {
        Pageable pageable = PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "rating"));

        List<Accommodation> accommodations;

        if (category != null && !category.isBlank() && !category.equals("전체")) {
            accommodations = accommodationRepository
                    .findByCategory_NameAndStatus(category, "active", pageable)
                    .getContent();
        } else {
            accommodations = accommodationRepository
                    .findByStatus("active", pageable)
                    .getContent();
        }

        return accommodations.stream().map(a -> AccommodationSummaryDTO.builder()
                .id(a.getAccommodationId())
                .title(a.getName())
                .location(a.getAddress())
                .image(a.getThumbnail())
                .price(a.getMinPrice())
                .rating(a.getRating())
                .category(a.getCategory().getName())
                .build()
        ).toList();
    }

    // 숙소 검색
    public PageResponseDTO<AccommodationSearchDTO> searchAvailable(
            String location,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            Long userId,
            String sort,
            List<String> selectedCategories,
            List<String> selectedTags,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        Page<AccommodationSearchDTO> page = accommodationRepository.searchAvailable(
                location,
                checkIn,
                checkOut,
                guests,
                userId,
                sort,
                selectedCategories,
                selectedTags,
                maxPrice,
                pageable
        );

        PageResponseDTO<AccommodationSearchDTO> dto = PageResponseDTO.<AccommodationSearchDTO>builder()
                .content(page.getContent())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .build();

        log.info("숙소 검색 결과 총 개수: {}", dto.getTotalElements());
        log.info("총 페이지 수: {}", dto.getTotalPages());
        log.info("현재 페이지 번호: {}", dto.getPage());
        log.info("페이지당 숙소 수: {}", dto.getSize());
        dto.getContent().forEach(accommodation -> {
            log.info("숙소 ID: {}, 데이터: {}", accommodation.getId(), accommodation.toString());
        });

        return dto;
    }

    //가격 범위 조회
    @Cacheable(value = "priceRange", unless = "#result == null")
    public Map<String, BigDecimal> getPriceRange() {
        BigDecimal maxPrice = accommodationRepository.findMaxMinPriceByStatus("ACTIVE");
        return Map.of("min", BigDecimal.ZERO, "max", maxPrice != null ? maxPrice : BigDecimal.ZERO);
    }

    public AccommodationDetailDTO getDetail(Long id, Long userId, LocalDate checkIn, LocalDate checkOut, Integer guests){
        return detailQueryRepository.fetchDetailById(id, userId, checkIn, checkOut, guests);
    }
}