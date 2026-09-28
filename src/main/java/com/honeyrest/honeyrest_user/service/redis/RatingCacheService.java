package com.honeyrest.honeyrest_user.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class RatingCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    public void saveRating(Long accommodationId, BigDecimal rating) {
        redisTemplate.opsForValue().set(AccommodationCacheKeys.rating(accommodationId), rating.toString());
    }

    public BigDecimal getRating(Long accommodationId) {
        String value = redisTemplate.opsForValue().get(AccommodationCacheKeys.rating(accommodationId));
        return value != null ? new BigDecimal(value) : null;
    }

    /**
     * 숙소에 딸린 캐시 키를 명시적으로 삭제한다.
     * KEYS 패턴 검색(O(N), Redis 블로킹)을 쓰지 않고 {@link AccommodationCacheKeys#allFor} 목록을 한 번의 DEL 로 지운다.
     */
    public void evictAllAccommodationCache(Long accommodationId) {
        List<String> keys = AccommodationCacheKeys.allFor(accommodationId);
        Long deleted = redisTemplate.delete(keys);
        log.debug("숙소 캐시 삭제: accommodationId={}, deleted={}", accommodationId, deleted);
    }
}
