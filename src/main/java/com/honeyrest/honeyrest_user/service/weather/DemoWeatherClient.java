package com.honeyrest.honeyrest_user.service.weather;

import lombok.extern.log4j.Log4j2;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * demo 프로필 전용 날씨 클라이언트. API 키가 없거나 호출이 실패하면 주요 도시의 예시 날씨를 돌려준다.
 * 같은 날·같은 도시는 항상 같은 값이고, 응답에 {@code sample=true} 를 붙인다.
 */
@Log4j2
public class DemoWeatherClient implements WeatherClient {

    /** 서울 월평균 기온(1~12월, 섭씨). */
    private static final double[] SEOUL_MONTHLY_AVG = {-2.0, 0.6, 5.8, 12.4, 17.8, 22.2, 24.9, 25.7, 21.2, 14.8, 7.2, 0.4};

    private record City(String korean, List<String> aliases, double lat, double lon, double tempOffset) {
        boolean matches(String query) {
            String q = query.trim().toLowerCase(Locale.ROOT);
            return korean.equals(query.trim()) || aliases.stream().anyMatch(a -> a.equalsIgnoreCase(q));
        }
    }

    private static final List<City> CITIES = List.of(
            new City("서울", List.of("seoul", "서울시", "서울특별시"), 37.5665, 126.9780, 0.0),
            new City("부산", List.of("busan", "부산시", "해운대"), 35.1796, 129.0756, 2.6),
            new City("제주", List.of("jeju", "제주도", "제주시", "애월"), 33.4996, 126.5312, 4.1),
            new City("서귀포", List.of("seogwipo", "서귀포시"), 33.2541, 126.5600, 4.8),
            new City("강릉", List.of("gangneung", "강릉시"), 37.7519, 128.8761, 0.9),
            new City("속초", List.of("sokcho", "속초시"), 38.2070, 128.5918, 0.2),
            new City("경주", List.of("gyeongju", "경주시"), 35.8562, 129.2247, 1.2),
            new City("인천", List.of("incheon"), 37.4563, 126.7052, -0.3),
            new City("대구", List.of("daegu"), 35.8714, 128.6014, 1.5),
            new City("대전", List.of("daejeon"), 36.3504, 127.3845, 0.4),
            new City("광주", List.of("gwangju"), 35.1595, 126.8526, 1.3),
            new City("울산", List.of("ulsan"), 35.5384, 129.3114, 1.6),
            new City("수원", List.of("suwon"), 37.2636, 127.0286, -0.2),
            new City("전주", List.of("jeonju"), 35.8242, 127.1480, 0.8),
            new City("여수", List.of("yeosu"), 34.7604, 127.6622, 2.2),
            new City("춘천", List.of("chuncheon"), 37.8813, 127.7298, -1.5),
            new City("포항", List.of("pohang"), 36.0190, 129.3435, 1.4)
    );

    /** 날씨 후보: 아이콘, 분류, 설명. 맑은 날이 더 자주 나오도록 앞쪽 항목을 여러 번 둔다. */
    private static final String[][] CONDITIONS = {
            {"01d", "Clear", "맑음"},
            {"01d", "Clear", "맑음"},
            {"02d", "Clouds", "구름 조금"},
            {"02d", "Clouds", "구름 조금"},
            {"03d", "Clouds", "구름 많음"},
            {"04d", "Clouds", "흐림"},
            {"10d", "Rain", "비"},
    };

    private final WeatherClient delegate;
    private final Clock clock;

    /** @param delegate 실제 날씨 클라이언트(키가 있을 때). 없으면 null → 항상 예시 데이터 */
    public DemoWeatherClient(WeatherClient delegate) {
        this(delegate, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    DemoWeatherClient(WeatherClient delegate, Clock clock) {
        this.delegate = delegate;
        this.clock = clock;
    }

    @Override
    public Optional<WeatherInfo> getCurrent(String city, Double lat, Double lon) {
        if (delegate != null) {
            try {
                return delegate.getCurrent(city, lat, lon);
            } catch (WeatherUnavailableException e) {
                log.warn("[demo] 실제 날씨 조회 실패 → 예시 데이터 사용: {}", e.getMessage());
            }
        }
        return sample(city, lat, lon);
    }

    Optional<WeatherInfo> sample(String city, Double lat, Double lon) {
        Optional<City> target;
        if (city != null && !city.isBlank()) {
            target = CITIES.stream().filter(c -> c.matches(city)).findFirst();
        } else if (lat != null && lon != null) {
            target = CITIES.stream().min((a, b) -> Double.compare(distance(a, lat, lon), distance(b, lat, lon)));
        } else {
            target = Optional.empty();
        }
        return target.map(this::sampleFor);
    }

    private WeatherInfo sampleFor(City city) {
        LocalDate today = LocalDate.now(clock);
        // 같은 날·같은 도시는 같은 값, 날짜가 바뀌면 조금씩 달라진다
        int seed = Math.floorMod((city.korean().hashCode() * 31) ^ (today.getDayOfYear() * 7919), Integer.MAX_VALUE);
        String[] condition = CONDITIONS[seed % CONDITIONS.length];
        double variation = ((seed / 7) % 50) / 10.0 - 2.5; // -2.5 ~ +2.4
        double temp = Math.round((SEOUL_MONTHLY_AVG[today.getMonthValue() - 1] + city.tempOffset() + variation) * 10) / 10.0;
        int humidity = 45 + (seed / 11) % 30 + ("Rain".equals(condition[1]) ? 20 : 0);
        return new WeatherInfo(city.korean(), temp, condition[1], condition[2], condition[0], Math.min(humidity, 95), true);
    }

    private static double distance(City c, double lat, double lon) {
        double dLat = c.lat() - lat;
        double dLon = (c.lon() - lon) * Math.cos(Math.toRadians(lat));
        return dLat * dLat + dLon * dLon;
    }
}
