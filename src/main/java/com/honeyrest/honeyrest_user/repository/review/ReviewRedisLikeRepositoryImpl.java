package com.honeyrest.honeyrest_user.repository.review;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 리뷰 좋아요 Redis 저장소.
 * <ul>
 *     <li>{@code review:like:{reviewId}} — 좋아요 수 카운터 (DB like_count 의 쓰기 버퍼)</li>
 *     <li>{@code review:like:users:{reviewId}} — 좋아요한 userId 집합. 사용자당 1회만 반영되도록 멱등성을 보장한다.</li>
 * </ul>
 */
@Repository
public class ReviewRedisLikeRepositoryImpl implements ReviewRedisLikeRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public ReviewRedisLikeRepositoryImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getKey(Long reviewId) {
        return "review:like:" + reviewId;
    }

    static String likersKey(Long reviewId) {
        return "review:like:users:" + reviewId;
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
        // 캐시가 없으면 호출자가 DB의 like_count를 유지할 수 있도록 null을 반환한다.
        return raw != null ? Integer.parseInt(raw.toString()) : null;
    }

    @Override
    public void initLikeCountIfAbsent(Long reviewId, int dbLikeCount) {
        redisTemplate.opsForValue().setIfAbsent(getKey(reviewId), Math.max(dbLikeCount, 0));
    }

    @Override
    public boolean addLiker(Long reviewId, Long userId) {
        Long added = redisTemplate.opsForSet().add(likersKey(reviewId), userId.toString());
        return added != null && added > 0;
    }

    @Override
    public boolean removeLiker(Long reviewId, Long userId) {
        Long removed = redisTemplate.opsForSet().remove(likersKey(reviewId), userId.toString());
        return removed != null && removed > 0;
    }
}
