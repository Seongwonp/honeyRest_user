package com.honeyrest.honeyrest_user.repository.review;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRedisLikeRepositoryImpl implements ReviewRedisLikeRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public ReviewRedisLikeRepositoryImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getKey(Long reviewId) {
        return "review:like:" + reviewId;
    }

    @Override
    public void increaseLikeCount(Long reviewId) {
        redisTemplate.opsForValue().increment(getKey(reviewId));
    }

    @Override
    public void decreaseLikeCount(Long reviewId) {
        String key = getKey(reviewId);
        Integer current = getLikeCount(reviewId);
        if (current != null && current > 0) {
            redisTemplate.opsForValue().decrement(key);
        }
    }

    @Override
    public Integer getLikeCount(Long reviewId) {
        Object raw = redisTemplate.opsForValue().get(getKey(reviewId));
        return raw != null ? Integer.parseInt(raw.toString()) : 0;
    }
}
