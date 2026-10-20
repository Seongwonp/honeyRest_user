package com.honeyrest.honeyrest_user.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 숙소 검색 결과 캐시(search:recommend:*)의 세대(version) 관리.
 * 검색 키는 조건이 많아 특정해 지울 수 없으므로, 재고·평점이 바뀌면 세대를 INCR 해 이전 키를 버린다(TTL 로 소멸).
 * Redis 장애가 예약/리뷰 트랜잭션을 깨지 않도록 예외는 로그만 남긴다.
 * 호스트 앱도 예약 취소 시 같은 키를 INCR 해야 한다.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class SearchCacheVersionService {

    public static final String VERSION_KEY = "search:recommend:version";

    private final RedisTemplate<String, String> redisTemplate;

    /** 현재 세대. 키가 없거나 Redis 를 읽을 수 없으면 0. */
    public long current() {
        try {
            String raw = redisTemplate.opsForValue().get(VERSION_KEY);
            return raw != null ? Long.parseLong(raw) : 0L;
        } catch (RuntimeException e) {
            log.warn("검색 캐시 세대 조회 실패, 0 으로 대체: {}", e.toString());
            return 0L;
        }
    }

    /** 세대를 올린다. 커밋 전에 올리면 옛 재고로 새 세대 캐시가 채워질 수 있어 트랜잭션 안이면 커밋 후에 올린다. */
    public void bumpAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    bump();
                }
            });
        } else {
            bump();
        }
    }

    void bump() {
        try {
            redisTemplate.opsForValue().increment(VERSION_KEY);
        } catch (RuntimeException e) {
            log.warn("검색 캐시 세대 증가 실패(최대 TTL 동안 오래된 검색 결과가 보일 수 있음): {}", e.toString());
        }
    }
}
