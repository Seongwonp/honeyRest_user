package com.honeyrest.honeyrest_user.service.email;

import com.honeyrest.honeyrest_user.exception.ApiException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인증/비밀번호 재설정/이메일 변경 메일 발송 남용 방지 (고정 윈도 카운터).
 * <p>
 * 같은 (용도, 이메일) 조합으로 {@code window} 안에 {@code maxRequests} 회를 넘으면 429 를 던진다.
 * 카운터는 Redis {@code rate:email:{용도}:{이메일}} 에 INCR + EXPIRE 로 두어 인스턴스 간 공유하고,
 * Redis 에 접근할 수 없으면(로컬/테스트 프로필, 장애) 인스턴스 로컬 메모리 카운터로 대체한다.
 * <p>
 * 계정 존재 여부를 노출하지 않도록 사용자 조회 <b>전에</b> 호출한다.
 */
@Log4j2
@Component
public class EmailRateLimiter {

    public enum Purpose { VERIFY, PASSWORD_RESET, EMAIL_CHANGE }

    private final RedisTemplate<String, String> redisTemplate;
    private final int maxRequests;
    private final Duration window;
    private final Clock clock;

    private final Map<String, LocalWindow> localCounters = new ConcurrentHashMap<>();

    @Autowired
    public EmailRateLimiter(RedisTemplate<String, String> redisTemplate,
                            @Value("${app.email.rate-limit.max-requests:3}") int maxRequests,
                            @Value("${app.email.rate-limit.window:10m}") Duration window) {
        this(redisTemplate, maxRequests, window, Clock.systemUTC());
    }

    EmailRateLimiter(RedisTemplate<String, String> redisTemplate, int maxRequests, Duration window, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    /** 허용 한도 내면 카운트를 1 올리고, 넘으면 429 ApiException. */
    public void checkAndRecord(Purpose purpose, String email) {
        if (email == null || email.isBlank()) {
            return; // 형식 오류는 호출자 검증에 맡긴다.
        }
        String key = "rate:email:" + purpose.name().toLowerCase(Locale.ROOT) + ":" + email.trim().toLowerCase(Locale.ROOT);
        long count = increment(key);
        if (count > maxRequests) {
            log.warn("메일 발송 한도 초과: purpose={}, count={}", purpose, count);
            throw new ApiException("요청이 너무 많습니다. " + window.toMinutes() + "분 후 다시 시도해 주세요.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private long increment(String key) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, window);
            }
            if (count != null) {
                return count;
            }
        } catch (RuntimeException e) {
            log.warn("메일 발송 한도 Redis 카운터 사용 불가, 로컬 카운터로 대체: {}", e.toString());
        }
        return incrementLocal(key);
    }

    private long incrementLocal(String key) {
        long now = clock.millis();
        LocalWindow w = localCounters.compute(key, (k, cur) ->
                (cur == null || now >= cur.expiresAt) ? new LocalWindow(now + window.toMillis(), 1) : cur.next());
        // 만료된 항목이 쌓이지 않도록 가끔 정리한다.
        if (localCounters.size() > 10_000) {
            localCounters.entrySet().removeIf(en -> now >= en.getValue().expiresAt);
        }
        return w.count;
    }

    private record LocalWindow(long expiresAt, long count) {
        LocalWindow next() {
            return new LocalWindow(expiresAt, count + 1);
        }
    }
}
