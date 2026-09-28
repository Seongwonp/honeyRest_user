package com.honeyrest.honeyrest_user.repository.accommodation;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccommodationSearchCacheKeyTest {

    @Test
    void cacheKeySeparatesAvailabilityPriceAndUserConditions() {
        String base = key(LocalDate.of(2026, 8, 10), 2, null, new BigDecimal("200000"));

        assertThat(key(LocalDate.of(2026, 8, 11), 2, null, new BigDecimal("200000"))).isNotEqualTo(base);
        assertThat(key(LocalDate.of(2026, 8, 10), 3, null, new BigDecimal("200000"))).isNotEqualTo(base);
        assertThat(key(LocalDate.of(2026, 8, 10), 2, 7L, new BigDecimal("200000"))).isNotEqualTo(base);
        assertThat(key(LocalDate.of(2026, 8, 10), 2, null, new BigDecimal("100000"))).isNotEqualTo(base);
    }

    @Test
    void cacheKeyNormalizesEquivalentCategoryAndTagOrder() {
        String first = AccommodationSearchImpl.buildCacheKey(
                " 서울 ", null, null,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11),
                2, null, "priceAsc",
                List.of("호텔", "한옥"), List.of("조용한", "무료주차"),
                new BigDecimal("200000.00"), PageRequest.of(0, 20)
        );
        String second = AccommodationSearchImpl.buildCacheKey(
                "서울", null, null,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11),
                2, null, "priceAsc",
                List.of("한옥", "호텔"), List.of("무료주차", "조용한"),
                new BigDecimal("200000"), PageRequest.of(0, 20)
        );

        assertThat(second).isEqualTo(first);
    }

    private String key(LocalDate checkIn, int guests, Long userId, BigDecimal maxPrice) {
        return AccommodationSearchImpl.buildCacheKey(
                "", null, null,
                checkIn, checkIn.plusDays(1), guests, userId, "priceAsc",
                List.of(), List.of(), maxPrice, PageRequest.of(0, 20)
        );
    }

    @Test
    void cacheKeyChangesWhenVersionIsBumped() {
        LocalDate checkIn = LocalDate.of(2026, 8, 10);
        String v1 = AccommodationSearchImpl.buildCacheKey(1L, "", null, null, checkIn, checkIn.plusDays(1),
                2, null, "priceAsc", List.of(), List.of(), null, PageRequest.of(0, 20));
        String v2 = AccommodationSearchImpl.buildCacheKey(2L, "", null, null, checkIn, checkIn.plusDays(1),
                2, null, "priceAsc", List.of(), List.of(), null, PageRequest.of(0, 20));

        // 예약 생성 등으로 세대가 오르면 같은 조건이라도 새 키를 쓰므로 이전 세대 결과가 재사용되지 않는다.
        assertThat(v2).isNotEqualTo(v1).startsWith("search:recommend:v3:ver=2:");
    }
}
