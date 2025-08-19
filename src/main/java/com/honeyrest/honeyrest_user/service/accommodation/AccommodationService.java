package com.honeyrest.honeyrest_user.service.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSearchDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSummaryDTO;
import com.honeyrest.honeyrest_user.entity.Accommodation;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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


    public Page<AccommodationSearchDTO> searchAvailable(
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
        Page<AccommodationSearchDTO> dto = accommodationRepository.searchAvailable(
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
        log.info("숙소 검색 결과 총 개수: {}", dto.getTotalElements());
        log.info("총 페이지 수: {}", dto.getTotalPages());
        log.info("현재 페이지 번호: {}", dto.getNumber());
        log.info("페이지당 숙소 수: {}", dto.getSize());
        dto.getContent().forEach(accommodation -> {
            log.info("숙소 ID: {}, 데이터: {}", accommodation.getId() ,accommodation.toString());
        });
        return dto;
    }

    public Map<String, BigDecimal> getPriceRange() {
        BigDecimal maxPrice = accommodationRepository.findMaxMinPriceByStatus("ACTIVE");
        return Map.of("min", BigDecimal.ZERO, "max", maxPrice != null ? maxPrice : BigDecimal.ZERO);
    }

}