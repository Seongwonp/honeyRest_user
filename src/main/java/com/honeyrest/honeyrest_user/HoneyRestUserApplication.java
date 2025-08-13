package com.honeyrest.honeyrest_user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class HoneyRestUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoneyRestUserApplication.class, args);
    }

}
