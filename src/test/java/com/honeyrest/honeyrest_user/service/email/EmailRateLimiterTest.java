package com.honeyrest.honeyrest_user.service.email;

import com.honeyrest.honeyrest_user.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailRateLimiterTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private static final Duration WINDOW = Duration.ofMinutes(10);

    @Test
    void Redis_카운터로_3회까지_허용하고_4회째_429() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String key = "rate:email:password_reset:user@example.com";
        when(valueOperations.increment(key)).thenReturn(1L, 2L, 3L, 4L);
        EmailRateLimiter limiter = new EmailRateLimiter(redisTemplate, 3, WINDOW);

        for (int i = 0; i < 3; i++) {
            // 대소문자/공백이 달라도 같은 이메일로 센다.
            assertThatCode(() -> limiter.checkAndRecord(EmailRateLimiter.Purpose.PASSWORD_RESET, " User@Example.com "))
                    .doesNotThrowAnyException();
        }
        assertThatThrownBy(() -> limiter.checkAndRecord(EmailRateLimiter.Purpose.PASSWORD_RESET, "user@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        // TTL 은 윈도 첫 요청에서만 건다.
        verify(redisTemplate, times(1)).expire(key, WINDOW);
    }

    @Test
    void Redis_장애시_로컬_카운터로_대체하고_윈도가_지나면_초기화된다() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));
        AtomicLong now = new AtomicLong(0);
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return Instant.ofEpochMilli(now.get()); }
        };
        EmailRateLimiter limiter = new EmailRateLimiter(redisTemplate, 3, WINDOW, clock);

        for (int i = 0; i < 3; i++) {
            limiter.checkAndRecord(EmailRateLimiter.Purpose.VERIFY, "a@example.com");
        }
        assertThatThrownBy(() -> limiter.checkAndRecord(EmailRateLimiter.Purpose.VERIFY, "a@example.com"))
                .isInstanceOf(ApiException.class);
        // 용도가 다르면 별도 카운터
        assertThatCode(() -> limiter.checkAndRecord(EmailRateLimiter.Purpose.PASSWORD_RESET, "a@example.com"))
                .doesNotThrowAnyException();

        now.set(WINDOW.toMillis());
        assertThatCode(() -> limiter.checkAndRecord(EmailRateLimiter.Purpose.VERIFY, "a@example.com"))
                .doesNotThrowAnyException();
        verify(redisTemplate, never()).expire(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
    }
}
