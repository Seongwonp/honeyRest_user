package com.honeyrest.honeyrest_user.e2e;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * e2e·demo 프로필 전용 Redis 대체 설정. RedisConfig 와 같은 빈 이름·타입으로 인메모리 템플릿을 등록한다.
 */
@Configuration
@Profile({"e2e", "demo"})
public class E2eRedisConfig {

    @Bean
    public InMemoryRedisTemplate.Store e2eRedisStore() {
        return new InMemoryRedisTemplate.Store();
    }

    /** RedisConfig#redisTemplate 대체 (값을 문자열로 읽는다: StringRedisSerializer 와 같은 관찰 결과). */
    @Bean
    public RedisTemplate<String, String> redisTemplate(InMemoryRedisTemplate.Store store) {
        return new InMemoryRedisTemplate<>(store, String::valueOf);
    }

    /** RedisConfig#objectRedisTemplate 대체 (값 객체를 그대로 돌려준다. 호출자는 ObjectMapper.convertValue 로 변환). */
    @Bean
    public RedisTemplate<String, Object> objectRedisTemplate(InMemoryRedisTemplate.Store store) {
        return new InMemoryRedisTemplate<>(store, v -> v);
    }
}
