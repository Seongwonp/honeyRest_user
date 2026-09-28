package com.honeyrest.honeyrest_user.service.redis;

import java.util.List;

/**
 * 숙소 단위 Redis 캐시 키 정의 (단일 출처).
 * <p>
 * 조회 쪽(AccommodationDetailQueryRepository, RatingCacheService)과 무효화 쪽(RatingCacheService.evictAllAccommodationCache)이
 * 같은 키 문자열을 쓰도록 여기서만 만든다. 과거에는 무효화가 {@code KEYS accommodation:*:{id}} 패턴에 의존해
 * reviewCount/reviewList/cancellationPolicy 키가 지워지지 않았고, KEYS 명령 자체도 운영 Redis 를 블로킹했다.
 * 키를 추가하면 {@link #allFor(Long)} 에도 반드시 추가한다 (docs/ARCHITECTURE.md 캐시 표 참고).
 */
public final class AccommodationCacheKeys {

    private AccommodationCacheKeys() {
    }

    public static String detail(Long id) {
        return "accommodation:detail:" + id;
    }

    public static String images(Long id) {
        return "accommodation:images:" + id;
    }

    public static String tags(Long id) {
        return "accommodation:tags:" + id;
    }

    /**
     * AccommodationTagMapService 전용 (AccommodationTagMapDTO 목록).
     * 과거에는 {@link #tags(Long)} 와 같은 키를 서로 다른 DTO 형태로 읽고 써서 캐시가 오염될 수 있었다.
     */
    public static String tagMap(Long id) {
        return "accommodation:tagmap:" + id;
    }

    public static String rating(Long id) {
        return "accommodation:rating:" + id;
    }

    public static String reviewCount(Long id) {
        return "reviewCount:accommodation:" + id;
    }

    public static String reviewList(Long id) {
        return "reviewList:accommodation:" + id;
    }

    public static String cancellationPolicy(Long id) {
        return "cancellationPolicy:accommodation:" + id;
    }

    /** 숙소 하나에 딸린 모든 캐시 키. */
    public static List<String> allFor(Long id) {
        return List.of(detail(id), images(id), tags(id), tagMap(id), rating(id),
                reviewCount(id), reviewList(id), cancellationPolicy(id));
    }
}
