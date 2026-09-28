package com.honeyrest.honeyrest_user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@EnableAsync
@SpringBootApplication
// JPA 엔티티는 공유 도메인 모듈(:honeyrest-domain, com.honeyrest.domain)에 있다. 앱 패키지 밖이라 명시적으로 스캔한다.
// (리포지토리 스캔은 기본값대로 앱 패키지 com.honeyrest.honeyrest_user 기준)
@EntityScan("com.honeyrest.domain")
public class HoneyRestUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoneyRestUserApplication.class, args);
    }

}
