package com.honeyrest.honeyrest_user.repository.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRedisLikeRepositoryImplTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private SetOperations<String, Object> setOperations;

    private ReviewRedisLikeRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        repository = new ReviewRedisLikeRepositoryImpl(redisTemplate);
    }

    @Test
    void getLikeCount_returnsNullWhenCacheDoesNotExist() {
        when(valueOperations.get("review:like:12")).thenReturn(null);

        assertThat(repository.getLikeCount(12L)).isNull();
    }

    @Test
    void getLikeCount_returnsCachedValueWhenPresent() {
        when(valueOperations.get("review:like:12")).thenReturn("23");

        assertThat(repository.getLikeCount(12L)).isEqualTo(23);
    }

    @Test
    void addLiker_returnsTrueOnlyWhenUserWasNewlyAdded() {
        when(setOperations.add("review:like:users:12", "7")).thenReturn(1L, 0L);

        assertThat(repository.addLiker(12L, 7L)).isTrue();
        // 같은 사용자의 두 번째 요청은 집합에 변화가 없으므로 false (카운터 증가 금지)
        assertThat(repository.addLiker(12L, 7L)).isFalse();
    }

    @Test
    void removeLiker_returnsFalseWhenUserNeverLiked() {
        when(setOperations.remove("review:like:users:12", "7")).thenReturn(0L);

        assertThat(repository.removeLiker(12L, 7L)).isFalse();
    }

    @Test
    void initLikeCountIfAbsent_seedsCounterFromDbValue() {
        repository.initLikeCountIfAbsent(12L, 23);

        verify(valueOperations).setIfAbsent("review:like:12", 23);
    }
}
