package com.honeyrest.honeyrest_user.service.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 날씨 클라이언트 배선.
 * <ul>
 *     <li>demo 외 프로필: {@link OpenWeatherClient} (키가 없으면 호출 없이 503)</li>
 *     <li>demo 프로필: {@link DemoWeatherClient} — 키가 있으면 실제 값, 없거나 실패하면 예시 데이터</li>
 * </ul>
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
