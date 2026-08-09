package com.honeyrest.honeyrest_user.repository.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRedisLikeRepositoryImplTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    private ReviewRedisLikeRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
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
}
