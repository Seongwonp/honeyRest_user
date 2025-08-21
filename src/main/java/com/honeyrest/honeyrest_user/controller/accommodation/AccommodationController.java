package com.honeyrest.honeyrest_user.controller.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationDetailDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSearchDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSummaryDTO;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.service.accommodation.AccommodationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.file.attribute.UserPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accommodations")
public class AccommodationController {

    private final AccommodationService accommodationService;

    @GetMapping("/popular")
    public ResponseEntity<List<AccommodationSummaryDTO>> getPopularAccommodations(
            @RequestParam(defaultValue = "전체") String category
    ) {
        log.info("인기 숙소 요청 - 카테고리: {}", category);
        List<AccommodationSummaryDTO> result = accommodationService.getPopularByCategory(category);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public PageResponseDTO<AccommodationSearchDTO> search(
            @RequestParam String location,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int guests,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "priceAsc") String sort,
            @RequestParam(required = false) List<String> selectedCategories,
            @RequestParam(required = false) List<String> selectedTags,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable
    ) {
        log.info("숙소 검색 요청: location={}, guests={}, sort={}, categories={}, tags={}, maxPrice={}",
                location, guests, sort, selectedCategories, selectedTags, maxPrice);

        return accommodationService.searchAvailable(
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
    }

    @GetMapping("/price-range")
    public ResponseEntity<Map<String, BigDecimal>> getPriceRange() {
        Map<String, BigDecimal> range = accommodationService.getPriceRange();
        return ResponseEntity.ok(range);
    }


    @GetMapping("/{id}")
    public ResponseEntity<AccommodationDetailDTO> getDetail(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Long userId
    ) {
        log.info("✅ 숙소 상세 요청: id={}, userId={}, checkIn={}, checkOut={}", id, userId, checkIn, checkOut);
        return ResponseEntity.ok(accommodationService.getDetail(id, userId, checkIn, checkOut));
    }


}