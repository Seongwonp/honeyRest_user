package com.honeyrest.honeyrest_user.controller.weather;

import com.honeyrest.honeyrest_user.service.weather.WeatherClient;
import com.honeyrest.honeyrest_user.service.weather.WeatherInfo;
import com.honeyrest.honeyrest_user.service.weather.WeatherUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * 홈 화면 날씨 위젯.
 * <ul>
 *     <li>200: {@code name, temp, condition, description, icon, humidity?, sample}</li>
 *     <li>400: 도시·좌표 둘 다 없음 / 404: 도시를 찾지 못함</li>
 *     <li>503: API 키 없음·외부 API 실패·시간 초과 → 프론트는 "불러올 수 없음 + 다시 시도"를 보여 준다</li>
 * </ul>
 */
@Log4j2
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherClient weatherClient;

    @GetMapping
    public ResponseEntity<?> getWeather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        boolean hasCity = city != null && !city.isBlank();
        if (!hasCity && (lat == null || lon == null)) {
            return error(HttpStatus.BAD_REQUEST, "도시 또는 좌표가 필요합니다.");
        }

        try {
            Optional<WeatherInfo> weather = weatherClient.getCurrent(hasCity ? city : null, lat, lon);
            if (weather.isEmpty()) {
                return error(HttpStatus.NOT_FOUND, "도시 정보를 찾을 수 없습니다.");
            }
            return ResponseEntity.ok(weather.get().toResponse());
        } catch (WeatherUnavailableException e) {
            // 키 미설정은 정상 운영 상태일 수 있으므로 스택 트레이스 없이 남긴다
            log.warn("날씨 정보를 가져오지 못함: {}", e.getMessage());
            return error(HttpStatus.SERVICE_UNAVAILABLE, "날씨 정보를 가져오지 못했습니다.");
        }
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }
}
