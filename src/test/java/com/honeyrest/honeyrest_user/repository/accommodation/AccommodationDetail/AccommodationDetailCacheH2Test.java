package com.honeyrest.honeyrest_user.repository.accommodation.AccommodationDetail;

import com.honeyrest.domain.entity.Accommodation;
import com.honeyrest.honeyrest_user.service.redis.AccommodationCacheKeys;
import com.honeyrest.honeyrest_user.support.H2IntegrationTestSupport;
import com.honeyrest.honeyrest_user.support.TestFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 숙소 상세 API: 날짜 생략 기본값, 404, 업체 주소, 캐시 TTL, 이미지 캐시 원자적 교체 (H2 + 인메모리 Redis). */
@DisplayName("숙소 상세: 날짜 기본값 · 업체 주소 · 캐시 TTL · 이미지 캐시 (H2)")
class AccommodationDetailCacheH2Test extends H2IntegrationTestSupport {

    @Autowired private TestRestTemplate rest;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private AccommodationDetailQueryRepository detailQueryRepository;
    @Autowired private ObjectMapper objectMapper;

    private Accommodation seed() {
        Accommodation acc = fixtures.accommodation(TestFixtures.unique("상세시"), "ACTIVE");
        fixtures.room(acc.getAccommodationId(), 2, 2, "ACTIVE");
        fixtures.image(acc.getAccommodationId(), "/images/a.jpg", 1);
        fixtures.image(acc.getAccommodationId(), "/images/b.jpg", 2);
        fixtures.cancellationPolicy(acc.getAccommodationId());
        return acc;
    }

    @Test
    @DisplayName("날짜 없이 상세를 조회해도 200 (오늘~내일 기준) 이고, 업체 주소는 숙소 주소가 아니라 업체 주소다")
    void detailWithoutDatesAndCompanyAddress() throws Exception {
        Accommodation acc = seed();

        ResponseEntity<String> response = rest.getForEntity("/api/accommodations/" + acc.getAccommodationId(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("company").path("address").asText()).isEqualTo("서울특별시 중구 업체로 1");
        assertThat(body.path("address").asText()).isEqualTo(acc.getAddress());
        assertThat(body.path("rooms").get(0).path("available").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("없는 숙소는 404, 체크아웃이 체크인보다 앞서면 400")
    void notFoundAndInvalidDates() {
        assertThat(rest.getForEntity("/api/accommodations/987654321", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        Accommodation acc = seed();
        assertThat(rest.getForEntity("/api/accommodations/" + acc.getAccommodationId()
                + "?checkIn=2099-01-05&checkOut=2099-01-04", String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("상세·이미지·태그·취소 정책 캐시는 모두 TTL(기본 10분)을 갖는다")
    void detailCachesHaveTtl() {
        Accommodation acc = seed();
        Long id = acc.getAccommodationId();

        rest.getForEntity("/api/accommodations/" + id, String.class);

        for (String key : List.of(AccommodationCacheKeys.detail(id), AccommodationCacheKeys.images(id),
                AccommodationCacheKeys.tags(id), AccommodationCacheKeys.cancellationPolicy(id))) {
            assertThat(redisTemplate.getExpire(key)).as("TTL of %s", key).isBetween(1L, 600L);
        }
        // 무효화 목록(호스트도 같은 목록으로 지운다)에 모두 포함되어 있어야 한다.
        assertThat(AccommodationCacheKeys.allFor(id)).contains(AccommodationCacheKeys.detail(id),
                AccommodationCacheKeys.images(id), AccommodationCacheKeys.tags(id), AccommodationCacheKeys.cancellationPolicy(id));
    }

    @Test
    @DisplayName("이미지 캐시는 통째로 교체된다: 동시에 MISS 난 두 요청이 써도 중복이 쌓이지 않는다")
    void imageCacheIsReplacedAtomically() {
        String key = AccommodationCacheKeys.images(TestFixtures.unique("").hashCode() & 0x7fffffffL);
        List<String> images = List.of("/images/a.jpg", "/images/b.jpg");

        // 과거 구현(이미지마다 RPUSH)이라면 두 번 쓰면 4개가 된다.
        detailQueryRepository.cacheImages(key, images);
        detailQueryRepository.cacheImages(key, images);

        assertThat(redisTemplate.opsForList().range(key, 0, -1)).containsExactlyElementsOf(images);
        assertThat(redisTemplate.getExpire(key)).isBetween(1L, 600L);
    }
}
