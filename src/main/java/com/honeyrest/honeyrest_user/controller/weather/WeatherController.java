package com.honeyrest.honeyrest_user.controller.weather;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    @Value("${weather.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public ResponseEntity<?> getWeather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        log.info("날씨 요청 수신: city={}, lat={}, lon={}", city, lat, lon);

        String url;

        if (city != null && !city.isEmpty()) {
            url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric",
                    city, apiKey
            );
        } else if (lat != null && lon != null) {
            url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric",
                    lat, lon, apiKey
            );
        } else {
            log.warn("잘못된 요청: 도시 또는 좌표가 누락됨");
            return ResponseEntity.badRequest().body("도시 또는 좌표가 필요합니다.");
        }

        log.debug("OpenWeather 요청 URL: {}", url);

        try {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            log.debug("OpenWeather 응답: {}", response);

            Object cod = response.get("cod");
            if (cod instanceof Integer && (Integer) cod != 200) {
                log.warn("도시 정보 없음: {}", response.get("message"));
                return ResponseEntity.badRequest().body("도시 정보를 찾을 수 없습니다.");
            }
            if (cod instanceof String && !"200".equals(cod)) {
                log.warn("도시 정보 없음: {}", response.get("message"));
                return ResponseEntity.badRequest().body("도시 정보를 찾을 수 없습니다.");
            }

            if (!response.containsKey("main") || !response.containsKey("weather")) {
                log.error("응답 데이터가 예상과 다름: {}", response);
                return ResponseEntity.status(500).body(Map.of("success", false, "message", "날씨 정보를 가져오지 못했습니다."));
            }

            Map<?, ?> main = (Map<?, ?>) response.get("main");
            Map<?, ?> weather = ((java.util.List<Map<?, ?>>) response.get("weather")).get(0);

            Map<String, Object> result = Map.of(
                    "name", response.get("name"),
                    "temp", main.get("temp"),
                    "condition", weather.get("main"),
                    "icon", weather.get("icon")
            );

            log.info("날씨 응답 결과: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("날씨 API 호출 중 오류 발생", e);
            return ResponseEntity.status(500).body("날씨 정보를 가져오지 못했습니다.");
        }
    }
}