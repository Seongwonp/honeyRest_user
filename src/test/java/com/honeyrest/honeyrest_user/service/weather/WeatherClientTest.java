package com.honeyrest.honeyrest_user.service.weather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("날씨 클라이언트: 키 없음은 즉시 실패, demo 는 예시 날씨로 대체")
class WeatherClientTest {

    private static final Clock OCT_18 = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    @DisplayName("API 키가 없으면 외부 호출 없이 WeatherUnavailableException")
    void openWeatherWithoutKeyFailsFast() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        OpenWeatherClient client = new OpenWeatherClient("  ", restTemplate);

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.getCurrent("Seoul", null, null))
                .isInstanceOf(WeatherUnavailableException.class);
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("demo: 키가 없으면 도시 이름(영문·한글)으로 예시 날씨를 돌려준다")
    void demoSampleByCity() {
        DemoWeatherClient client = new DemoWeatherClient(null, OCT_18);

        WeatherInfo seoul = client.getCurrent("Seoul", null, null).orElseThrow();
        assertThat(seoul.name()).isEqualTo("서울");
        assertThat(seoul.sample()).isTrue();
        assertThat(seoul.temp()).isBetween(5.0, 25.0); // 10월 서울 평균 14.8 ± 보정
        assertThat(seoul.icon()).matches("\\d\\dd");
        assertThat(seoul.humidity()).isBetween(30, 95);

        assertThat(client.getCurrent("제주", null, null)).get().extracting(WeatherInfo::name).isEqualTo("제주");
        // 같은 날 같은 도시는 같은 값
        assertThat(client.getCurrent("서울", null, null)).contains(seoul);
    }

    @Test
    @DisplayName("demo: 좌표는 가장 가까운 도시, 모르는 도시는 empty")
    void demoSampleByCoordsAndUnknownCity() {
        DemoWeatherClient client = new DemoWeatherClient(null, OCT_18);

        assertThat(client.getCurrent(null, 35.16, 129.16)).get().extracting(WeatherInfo::name).isEqualTo("부산");
        assertThat(client.getCurrent("Atlantis", null, null)).isEmpty();
    }

    @Test
    @DisplayName("demo: 실제 클라이언트가 있으면 그 값을 쓰고, 실패하면 예시 날씨로 대체한다")
    void demoUsesDelegateThenFallsBack() {
        WeatherClient real = mock(WeatherClient.class);
        WeatherInfo live = new WeatherInfo("서울", 12.3, "Clear", "맑음", "01d", 40, false);
        when(real.getCurrent(any(), any(), any())).thenReturn(Optional.of(live));
        assertThat(new DemoWeatherClient(real, OCT_18).getCurrent("Seoul", null, null)).contains(live);

        WeatherClient broken = mock(WeatherClient.class);
        when(broken.getCurrent(any(), any(), any())).thenThrow(new WeatherUnavailableException("timeout"));
        assertThat(new DemoWeatherClient(broken, OCT_18).getCurrent("Seoul", null, null))
                .get().extracting(WeatherInfo::sample).isEqualTo(true);
    }
}
