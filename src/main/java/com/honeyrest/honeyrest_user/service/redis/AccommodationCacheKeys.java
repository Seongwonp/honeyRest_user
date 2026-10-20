package com.honeyrest.honeyrest_user.service.redis;

import java.util.List;

/**
 * 숙소 단위 Redis 캐시 키 정의. 조회와 무효화가 같은 키를 쓰도록 여기서만 만든다.
 * 키를 추가하면 {@link #allFor(Long)} 에도 추가한다.
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

    /** AccommodationTagMapService 전용 (AccommodationTagMapDTO 목록). {@link #tags(Long)} 와 키를 나눈다. */
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
