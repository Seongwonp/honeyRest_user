package com.honeyrest.honeyrest_user.service.weather;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OpenWeather "현재 날씨" API 클라이언트. 키가 없으면 호출 없이 {@link WeatherUnavailableException}.
 * 연결 3초 / 응답 5초 제한, 도시를 찾지 못하면 empty.
 */
@Log4j2
public class OpenWeatherClient implements WeatherClient {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    private final String apiKey;
    private final RestTemplate restTemplate;

    public OpenWeatherClient(String apiKey) {
        this(apiKey, defaultRestTemplate());
    }

    OpenWeatherClient(String apiKey, RestTemplate restTemplate) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.restTemplate = restTemplate;
    }

    private static RestTemplate defaultRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    @Override
    public Optional<WeatherInfo> getCurrent(String city, Double lat, Double lon) {
        if (!isConfigured()) {
            throw new WeatherUnavailableException("날씨 API 키가 설정되지 않았습니다 (weather.api.key)");
        }

        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .queryParam("lang", "kr");
        if (city != null && !city.isBlank()) {
            uri.queryParam("q", city.trim());
        } else {
            uri.queryParam("lat", lat).queryParam("lon", lon);
        }
        URI url = uri.encode().build().toUri();

        Map<?, ?> response;
        try {
            response = restTemplate.getForObject(url, Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new WeatherUnavailableException("날씨 API 호출 실패: " + e.getMessage(), e);
        }

        if (response == null || !(response.get("main") instanceof Map<?, ?> main)
                || !(response.get("weather") instanceof List<?> weatherList) || weatherList.isEmpty()
                || !(weatherList.get(0) instanceof Map<?, ?> weather)) {
            log.warn("날씨 API 응답 형식이 예상과 다름: {}", response);
            throw new WeatherUnavailableException("날씨 API 응답 형식이 예상과 다릅니다");
        }

        Integer humidity = main.get("humidity") instanceof Number n ? n.intValue() : null;
        double temp = main.get("temp") instanceof Number t ? t.doubleValue() : 0.0;
        return Optional.of(new WeatherInfo(
                translateCityName(String.valueOf(response.get("name"))),
                temp,
                String.valueOf(weather.get("main")),
                String.valueOf(weather.get("description")),
                String.valueOf(weather.get("icon")),
                humidity,
                false));
    }

    static String translateCityName(String name) {
        return switch (name) {
            case "Seoul" -> "서울";
            case "Busan" -> "부산";
            case "Daegu" -> "대구";
            case "Incheon" -> "인천";
            case "Gwangju" -> "광주";
            case "Daejeon" -> "대전";
            case "Ulsan" -> "울산";
            default -> name;
        };
    }
}
