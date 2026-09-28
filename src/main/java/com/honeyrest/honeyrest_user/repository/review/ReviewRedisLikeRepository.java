package com.honeyrest.honeyrest_user.repository.review;


public interface ReviewRedisLikeRepository {
    void increaseLikeCount(Long reviewId);
    void decreaseLikeCount(Long reviewId);
    Integer getLikeCount(Long reviewId);

    /**
     * 카운터 키가 없을 때만 DB 의 like_count 로 초기화한다.
     * 캐시 미스 상태에서 INCR 하면 0 부터 세어 DB 값이 1 로 덮어써지는 문제를 막는다.
     */
    void initLikeCountIfAbsent(Long reviewId, int dbLikeCount);

    /** 사용자를 좋아요 집합에 추가한다. 새로 추가됐으면 true, 이미 있었으면 false. */
    boolean addLiker(Long reviewId, Long userId);

    /** 사용자를 좋아요 집합에서 제거한다. 실제로 제거됐으면 true, 없었으면 false. */
    boolean removeLiker(Long reviewId, Long userId);
}
