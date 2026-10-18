package com.honeyrest.honeyrest_user.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * demo 프로필 전용: 기동 직후 "인기 숙소 / 인기 지역" 점수를 채운다.
 * <p>
 * 두 목록은 원래 사용자 조회·검색이 쌓은 Redis ZSet 점수로 만든다
 * ({@code AccommodationService#getPopularByCategory}, {@code RegionService#getTopRegions}).
 * demo 는 인메모리 Redis 대체(재시작 시 비어 있음)라 첫 화면이 비어 보이지 않도록 시드 숙소의 평점·리뷰 수로 점수를 넣는다.
 * 키 형식은 서비스 코드와 같다: {@code popular:accommodation:{카테고리명}}, {@code regions:popular:count}.
 */
@Log4j2
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoPopularityWarmup implements ApplicationRunner {

    static final String POPULAR_ACCOMMODATION_PREFIX = "popular:accommodation:";
    static final String POPULAR_REGION_KEY = "regions:popular:count";

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT a.accommodation_id AS id, c.name AS category, r.name AS region,
                       COALESCE(a.rating, 0) AS rating,
                       (SELECT COUNT(*) FROM review rv WHERE rv.accommodation_id = a.accommodation_id) AS reviews
                  FROM accommodation a
                  JOIN accommodation_category c ON c.category_id = a.category_id
                  JOIN region r ON r.region_id = a.main_region_id
                 WHERE UPPER(a.status) = 'ACTIVE'
                """);

        for (Map<String, Object> row : rows) {
            String id = String.valueOf(row.get("id"));
            double score = ((Number) row.get("rating")).doubleValue() * 10 + ((Number) row.get("reviews")).doubleValue();
            objectRedisTemplate.opsForZSet().incrementScore(POPULAR_ACCOMMODATION_PREFIX + row.get("category"), id, score);
            objectRedisTemplate.opsForZSet().incrementScore(POPULAR_REGION_KEY, String.valueOf(row.get("region")), score);
        }
        log.info("[demo] 인기 숙소/지역 점수 초기화: 숙소 {}곳", rows.size());
    }
}
