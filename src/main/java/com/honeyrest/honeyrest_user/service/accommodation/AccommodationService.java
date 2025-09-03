package com.honeyrest.honeyrest_user.service.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationDetailDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSearchDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSummaryDTO;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.entity.Accommodation;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationRepository;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationDetail.AccommodationDetailQueryRepository;
import com.honeyrest.honeyrest_user.repository.review.ReviewRepository;
import com.honeyrest.honeyrest_user.service.redis.RatingCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final AccommodationDetailQueryRepository detailQueryRepository;
    private final ReviewRepository reviewRepository;
    private final RatingCacheService ratingCacheService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String POPULAR_KEY_PREFIX = "popular:accommodation:";

    /**
     * 인기 숙소 조회 (카테고리별, Redis ZSet 활용)
     */
    public List<AccommodationSummaryDTO> getPopularByCategory(String category) {
        // 전체 조회(카테고리 "전체" 또는 null)일 때 모든 카테고리 ZSet에서 상위 6개씩 모아서 반환
        if (category == null || category.isBlank() || "전체".equals(category)) {
            log.info("전체 인기 숙소를 조회합니다. 모든 카테고리 ZSet에서 상위 6개씩 가져옵니다.");
            // Redis에서 모든 인기 카테고리 키 조회
            Set<String> keys = redisTemplate.keys(POPULAR_KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                log.info("Redis에서 인기 숙소 카테고리 키를 찾지 못했습니다.");
                return Collections.emptyList();
            }
            // 각 카테고리별로 상위 6개 ID 추출
            Set<String> uniqueIds = new LinkedHashSet<>();
            for (String key : keys) {
                Set<Object> ids = redisTemplate.opsForZSet().reverseRange(key, 0, 5);
                if (ids != null && !ids.isEmpty()) {
                    log.info("Redis ZSet '{}'에서 상위 {}개의 인기 숙소 ID를 조회했습니다.", key, ids.size());
                    ids.forEach(obj -> uniqueIds.add(obj.toString()));
                } else {
                    log.info("Redis ZSet '{}'에서 인기 숙소를 찾지 못했습니다.", key);
                }
            }
            if (uniqueIds.isEmpty()) {
                log.info("모든 카테고리에서 인기 숙소를 찾지 못했습니다.");
                return Collections.emptyList();
            }
            // 최대 30개까지만 반환(카테고리 5개 * 6개 등)
            List<Long> accommodationIds = uniqueIds.stream()
                    .limit(30)
                    .map(Long::parseLong)
                    .toList();
            List<Accommodation> accommodations = accommodationRepository.findAllById(accommodationIds);
            Map<Long, Accommodation> accMap = new HashMap<>();
            accommodations.forEach(a -> accMap.put(a.getAccommodationId(), a));
            List<AccommodationSummaryDTO> result = new ArrayList<>();
            for (Long id : accommodationIds) {
                Accommodation a = accMap.get(id);
                if (a != null) {
                    result.add(AccommodationSummaryDTO.builder()
                            .id(a.getAccommodationId())
                            .title(a.getName())
                            .location(a.getAddress())
                            .image(a.getThumbnail())
                            .price(a.getMinPrice())
                            .rating(a.getRating())
                            .category(a.getCategory().getName())
                            .build()
                    );
                }
            }
            log.info("전체 인기 숙소 {}개를 반환합니다.", result.size());
            return result;
        } else {
            String key = POPULAR_KEY_PREFIX + category;
            log.info("카테고리 '{}'의 인기 숙소를 조회합니다. Redis 키: '{}'", category, key);
            Set<Object> topIds = redisTemplate.opsForZSet().reverseRange(key, 0, 5);
            if (topIds == null || topIds.isEmpty()) {
                log.info("Redis ZSet에서 '{}' 키로 인기 숙소를 찾지 못했습니다.", key);
                return Collections.emptyList();
            } else {
                log.info("Redis ZSet '{}'에서 {}개의 인기 숙소 ID를 성공적으로 조회했습니다.", key, topIds.size());
            }
            List<Long> accommodationIds = topIds.stream()
                    .map(obj -> Long.parseLong(obj.toString()))
                    .toList();
            List<Accommodation> accommodations = accommodationRepository.findAllById(accommodationIds);
            Map<Long, Accommodation> accMap = new HashMap<>();
            accommodations.forEach(a -> accMap.put(a.getAccommodationId(), a));
            List<AccommodationSummaryDTO> result = new ArrayList<>();
            for (Long id : accommodationIds) {
                Accommodation a = accMap.get(id);
                if (a != null) {
                    result.add(AccommodationSummaryDTO.builder()
                            .id(a.getAccommodationId())
                            .title(a.getName())
                            .location(a.getAddress())
                            .image(a.getThumbnail())
                            .price(a.getMinPrice())
                            .rating(a.getRating())
                            .category(a.getCategory().getName())
                            .build()
                    );
                }
            }
            log.info("카테고리 '{}'의 인기 숙소 {}개를 반환합니다.", category, result.size());
            return result;
        }
    }

    /** 인기 숙소 조회수 증가 (ZSet score 증가) */
    public void incrementPopularity(Long accommodationId, String category) {
        String key = POPULAR_KEY_PREFIX + (category == null || category.isBlank() || "전체".equals(category) ? "all" : category);
        log.info("숙소 ID '{}'의 인기 점수를 증가시킵니다. 대상 카테고리: '{}', Redis 키: '{}'", accommodationId, category, key);
        redisTemplate.opsForZSet().incrementScore(key, accommodationId.toString(), 1.0);
        log.info("숙소 ID '{}'의 인기 점수가 Redis ZSet '{}'에서 성공적으로 증가하였습니다.", accommodationId, key);
    }

    // 숙소 검색
    public PageResponseDTO<AccommodationSearchDTO> searchAvailable(
            String location,
            Double lat,
            Double lng,
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
    public Map<String, BigDecimal> getPriceRange() {
        BigDecimal maxPrice = accommodationRepository.findMaxMinPriceByStatus("ACTIVE");
        return Map.of("min", BigDecimal.ZERO, "max", maxPrice != null ? maxPrice : BigDecimal.ZERO);
    }

    public AccommodationDetailDTO getDetail(Long id, Long userId, LocalDate checkIn, LocalDate checkOut, Integer guests){
        AccommodationDetailDTO dto = detailQueryRepository.fetchDetailById(id, userId, checkIn, checkOut, guests);
        incrementPopularity(dto.getId(),dto.getCategory());
        return dto;
    }

    @Transactional
    public void updateRating(Long accommodationId) {
        // 1. 리뷰 평점 목록 조회
        List<BigDecimal> ratings = reviewRepository.findRatingsByAccommodationId(accommodationId);
        if (ratings.isEmpty()) return;

        // 2. 평균 계산
        BigDecimal sum = ratings.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);

        // 3. Redis에 캐싱
        ratingCacheService.saveRating(accommodationId, average);

        // 4. DB에 반영
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new IllegalArgumentException("숙소 정보를 찾을 수 없습니다"));
        accommodation.updateRating(average); // 엔티티에 커스텀 메서드 추가
    }

}