package com.honeyrest.honeyrest_user.repository.review;


public interface ReviewRedisLikeRepository {
    void increaseLikeCount(Long reviewId);
    void decreaseLikeCount(Long reviewId);
    Integer getLikeCount(Long reviewId);
}
