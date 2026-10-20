package com.honeyrest.honeyrest_user.service.weather;

import java.util.Optional;

/**
 * 현재 날씨 조회. 구현은 {@link OpenWeatherClient}, demo 는 {@link DemoWeatherClient} ({@link WeatherClientConfig}).
 */
public interface WeatherClient {

    /**
     * @param city 도시 이름 (영문 또는 한글). 비어 있으면 좌표로 조회한다.
     * @return 날씨. 도시를 찾지 못하면 empty.
     * @throws WeatherUnavailableException API 키가 없거나 외부 호출이 실패·시간 초과된 경우
     */
    Optional<WeatherInfo> getCurrent(String city, Double lat, Double lon);
}
