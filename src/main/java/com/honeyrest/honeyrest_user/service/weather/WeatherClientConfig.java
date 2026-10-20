package com.honeyrest.honeyrest_user.service.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 날씨 클라이언트 배선. demo 프로필만 {@link DemoWeatherClient} 로 감싼다.
 */
@Configuration
public class WeatherClientConfig {

    @Bean
    @Profile("!demo")
    public WeatherClient openWeatherClient(@Value("${weather.api.key:}") String apiKey) {
        return new OpenWeatherClient(apiKey);
    }

    @Bean
    @Profile("demo")
    public WeatherClient demoWeatherClient(@Value("${weather.api.key:}") String apiKey) {
        OpenWeatherClient real = new OpenWeatherClient(apiKey);
        return new DemoWeatherClient(real.isConfigured() ? real : null);
    }
}
