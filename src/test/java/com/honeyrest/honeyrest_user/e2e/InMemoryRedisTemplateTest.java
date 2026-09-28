package com.honeyrest.honeyrest_user.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryRedisTemplate(e2e 인메모리 Redis) 테스트")
class InMemoryRedisTemplateTest {

    private final InMemoryRedisTemplate.Store store = new InMemoryRedisTemplate.Store();
    private final InMemoryRedisTemplate<String> strings = new InMemoryRedisTemplate<>(store, String::valueOf);
    private final InMemoryRedisTemplate<Object> objects = new InMemoryRedisTemplate<>(store, v -> v);

    @Test
    @DisplayName("값 get/set/increment 와 두 템플릿의 키 공간 공유")
    void valueOps() {
        objects.opsForValue().set("k", List.of("a"));
        assertThat(objects.opsForValue().get("k")).isEqualTo(List.of("a"));

        assertThat(strings.opsForValue().increment("counter")).isEqualTo(1L);
        assertThat(strings.opsForValue().increment("counter")).isEqualTo(2L);
        assertThat(strings.opsForValue().get("counter")).isEqualTo("2");
        assertThat(objects.opsForValue().setIfAbsent("counter", 10)).isFalse();

        assertThat(objects.delete(List.of("k", "counter", "missing"))).isEqualTo(2L);
        assertThat(objects.hasKey("k")).isFalse();
    }

    @Test
    @DisplayName("TTL 이 지나면 값이 사라진다")
    void ttl() throws InterruptedException {
        objects.opsForValue().set("short", "v", Duration.ofMillis(20));
        Thread.sleep(40);
        assertThat(objects.opsForValue().get("short")).isNull();
    }

    @Test
    @DisplayName("리스트·집합·정렬 집합·scan")
    void collections() {
        objects.opsForList().rightPush("list", "a");
        objects.opsForList().rightPush("list", "b");
        assertThat(objects.opsForList().range("list", 0, -1)).containsExactly("a", "b");

        assertThat(objects.opsForSet().add("likers", "1")).isEqualTo(1L);
        assertThat(objects.opsForSet().add("likers", "1")).isEqualTo(0L);
        assertThat(objects.opsForSet().remove("likers", "1")).isEqualTo(1L);

        objects.opsForZSet().incrementScore("popular:all", "10", 1.0);
        objects.opsForZSet().incrementScore("popular:all", "20", 3.0);
        assertThat(objects.opsForZSet().reverseRange("popular:all", 0, 5)).containsExactly("20", "10");
        Set<ZSetOperations.TypedTuple<Object>> top = objects.opsForZSet().reverseRangeWithScores("popular:all", 0, 0);
        assertThat(top).singleElement().satisfies(t -> assertThat(t.getScore()).isEqualTo(3.0));

        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = objects.scan(ScanOptions.scanOptions().match("popular:*").build())) {
            cursor.forEachRemaining(keys::add);
        }
        assertThat(keys).containsExactly("popular:all");
    }

    @Test
    @DisplayName("지원하지 않는 연산은 바로 드러난다")
    void unsupported() {
        assertThatThrownBy(() -> objects.opsForValue().getAndDelete("k"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("ValueOperations.getAndDelete");
    }
}
