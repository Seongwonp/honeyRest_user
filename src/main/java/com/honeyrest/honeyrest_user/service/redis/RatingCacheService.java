package com.honeyrest.honeyrest_user.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
}