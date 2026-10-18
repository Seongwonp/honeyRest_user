package com.honeyrest.honeyrest_user.service.weather;

/** 날씨 정보를 가져올 수 없음 (API 키 없음, 외부 API 오류·시간 초과). 컨트롤러가 503 으로 응답한다. */
public class WeatherUnavailableException extends RuntimeException {

    public WeatherUnavailableException(String message) {
        super(message);
    }

    public WeatherUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
