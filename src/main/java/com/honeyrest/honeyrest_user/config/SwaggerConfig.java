package com.honeyrest.honeyrest_user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI honeyRestOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HoneyRest User API")
                        .description("API documentation for HoneyRest user-facing services")
                        .version("v1.0.0"));
    }
}
