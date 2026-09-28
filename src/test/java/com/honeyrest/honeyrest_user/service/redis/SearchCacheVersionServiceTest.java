package com.honeyrest.honeyrest_user.service.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCacheVersionServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @AfterEach
    void clearSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 트랜잭션_안에서는_커밋_후에_세대를_올린다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        SearchCacheVersionService service = new SearchCacheVersionService(redisTemplate);
        TransactionSynchronizationManager.initSynchronization();

        service.bumpAfterCommit();
        verify(valueOperations, never()).increment(SearchCacheVersionService.VERSION_KEY);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(valueOperations).increment(SearchCacheVersionService.VERSION_KEY);
    }

    @Test
    void 트랜잭션_밖에서는_즉시_올린다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        new SearchCacheVersionService(redisTemplate).bumpAfterCommit();

        verify(valueOperations).increment(SearchCacheVersionService.VERSION_KEY);
    }

    @Test
    void Redis_장애는_예약_흐름을_깨지_않는다() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));
        SearchCacheVersionService service = new SearchCacheVersionService(redisTemplate);

        assertThatCode(service::bumpAfterCommit).doesNotThrowAnyException();
        assertThat(service.current()).isZero();
    }
}
