package com.honeyrest.honeyrest_user.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 숙소 검색 결과 캐시(search:recommend:*)의 세대(version) 관리.
 * <p>
 * 검색 캐시 키에는 체크인/체크아웃·인원·필터·페이지가 모두 들어가므로, 예약 1건이 영향을 주는 키를
 * 특정해 지울 수 없다. KEYS/SCAN 으로 패턴 삭제하는 대신 키에 세대 번호를 넣고,
 * 재고·평점이 바뀌면 세대를 INCR 해서 이전 세대 키를 모두 "고아"로 만든다(남은 키는 TTL 6시간 후 자연 소멸).
 * <p>
 * Redis 장애 시에도 예약/리뷰 트랜잭션이 실패하지 않도록 예외를 삼키고 로그만 남긴다.
 * <b>호스트 저장소(honeyRest_host)에서 예약을 취소(CANCELLED)할 때도 같은 키를 INCR 해야 검색 재고가 즉시 반영된다.</b>
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

    /**
     * 세대를 올린다. 트랜잭션 안이면 커밋 이후에 올린다.
     * (커밋 전에 올리면 그 사이 다른 요청이 아직 커밋되지 않은 옛 재고로 새 세대 캐시를 채울 수 있다.)
     */
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
