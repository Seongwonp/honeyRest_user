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
}
