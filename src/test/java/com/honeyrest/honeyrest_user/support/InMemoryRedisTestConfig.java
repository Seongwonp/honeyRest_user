package com.honeyrest.honeyrest_user.support;

import com.honeyrest.honeyrest_user.e2e.InMemoryRedisTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 테스트 전용: 실제 Redis 대신 e2e 프로필의 인메모리 RedisTemplate 을 @Primary 로 등록한다.
 * 서비스 흐름(캐시 쓰기·무효화·검색 세대 증가)을 Redis 서버 없이 그대로 실행하고 TTL 까지 확인할 수 있다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class InMemoryRedisTestConfig {

    @Bean
    public InMemoryRedisTemplate.Store testRedisStore() {
        return new InMemoryRedisTemplate.Store();
    }

    @Bean
    @Primary
    public RedisTemplate<String, String> testStringRedisTemplate(InMemoryRedisTemplate.Store store) {
        return new InMemoryRedisTemplate<>(store, String::valueOf);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> testObjectRedisTemplate(InMemoryRedisTemplate.Store store) {
        return new InMemoryRedisTemplate<>(store, v -> v);
    }
}
