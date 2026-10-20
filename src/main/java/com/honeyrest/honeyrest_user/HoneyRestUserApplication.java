package com.honeyrest.honeyrest_user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@EnableAsync
@SpringBootApplication
// JPA 엔티티는 도메인 모듈(com.honeyrest.domain)에 있어 앱 패키지 밖이므로 명시적으로 스캔한다.
@EntityScan("com.honeyrest.domain")
public class HoneyRestUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoneyRestUserApplication.class, args);
    }

}
