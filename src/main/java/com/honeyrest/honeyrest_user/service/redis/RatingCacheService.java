package com.honeyrest.honeyrest_user.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RatingCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    public void saveRating(Long accommodationId, BigDecimal rating) {
        redisTemplate.opsForValue().set("accommodation:rating:" + accommodationId, rating.toString());
    }

    public BigDecimal getRating(Long accommodationId) {
        String value = redisTemplate.opsForValue().get("accommodation:rating:" + accommodationId);
        return value != null ? new BigDecimal(value) : null;
    }

    public void evictAllAccommodationCache(Long accommodationId) {
        String pattern = "accommodation:*:" + accommodationId;
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null) {
            for (String key : keys) {
                redisTemplate.delete(key);
                System.out.println("Deleted cache key: " + key);
            }
        }
    }
}