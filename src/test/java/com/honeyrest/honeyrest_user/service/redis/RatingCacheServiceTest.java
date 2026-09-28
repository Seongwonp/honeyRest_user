package com.honeyrest.honeyrest_user.service.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RatingCacheServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void 숙소_캐시_무효화는_KEYS_없이_조회측과_같은_키를_모두_지운다() {
        RatingCacheService service = new RatingCacheService(redisTemplate);

        service.evictAllAccommodationCache(7L);

        verify(redisTemplate, never()).keys(anyString());
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate).delete(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(
                "accommodation:detail:7",
                "accommodation:images:7",
                "accommodation:tags:7",
                "accommodation:tagmap:7",
                "accommodation:rating:7",
                // 과거 KEYS accommodation:*:{id} 패턴으로는 지워지지 않던 키들
                "reviewCount:accommodation:7",
                "reviewList:accommodation:7",
                "cancellationPolicy:accommodation:7");
    }
}
