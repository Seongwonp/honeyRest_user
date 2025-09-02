package com.honeyrest.honeyrest_user.controller.accommodation;

import com.honeyrest.honeyrest_user.dto.CancellationPolicyDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationDetailDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSearchDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSummaryDTO;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.service.CancellationPolicyService;
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
    private final CancellationPolicyService cancellationPolicyService;

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
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
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
        log.info("숙소 검색 요청: location={}, lat={}, lng={}, guests={}, sort={}, categories={}, tags={}, maxPrice={}",
                location, lat, lng, guests, sort, selectedCategories, selectedTags, maxPrice);

        return accommodationService.searchAvailable(
                location,
                lat,
                lng,
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
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Long userId
    ) {
        log.info("✅ 숙소 상세 요청: id={}, userId={}, checkIn={}, checkOut={}, guests={}", id, userId, checkIn, checkOut, guests);
        return ResponseEntity.ok(accommodationService.getDetail(id, userId, checkIn, checkOut, guests));
    }


    @GetMapping("/{id}/cancellation-policies")
    public ResponseEntity<List<CancellationPolicyDTO>> getCancellationPolicies(@PathVariable Long id) {
        log.info("🔍 숙소 취소 규정 조회: accommodationId={}", id);
        List<CancellationPolicyDTO> policies = cancellationPolicyService.getCancellationPoliciesByAccommodationId(id);
        return ResponseEntity.ok(policies);
    }

}