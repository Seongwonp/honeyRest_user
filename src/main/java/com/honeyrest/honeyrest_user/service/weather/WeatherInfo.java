package com.honeyrest.honeyrest_user.service.weather;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 날씨 위젯 응답.
 *
 * @param name        표시용 도시 이름
 * @param temp        기온(섭씨)
 * @param condition   날씨 분류(영문, 예: Clear)
 * @param description 날씨 설명(한글, 예: 맑음)
 * @param icon        OpenWeather 아이콘 코드(예: 01d)
 * @param humidity    습도(%), 모르면 null
 * @param sample      실제 관측값이 아닌 예시 데이터(demo 프로필)이면 true
 */
public record WeatherInfo(String name, double temp, String condition, String description, String icon,
                          Integer humidity, boolean sample) {

    /** name/temp/condition/description/icon/humidity/sample JSON 본문. */
    public Map<String, Object> toResponse() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("temp", temp);
        body.put("condition", condition);
        body.put("description", description);
        body.put("icon", icon);
        if (humidity != null) {
            body.put("humidity", humidity);
        }
        body.put("sample", sample);
        return body;
    }
}
