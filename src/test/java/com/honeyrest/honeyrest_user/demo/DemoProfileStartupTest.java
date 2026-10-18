package com.honeyrest.honeyrest_user.demo;

import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.e2e.E2eNoOpEmailService;
import com.honeyrest.honeyrest_user.e2e.E2eSupportController;
import com.honeyrest.honeyrest_user.e2e.E2eTossClient;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.service.email.EmailService;
import com.honeyrest.honeyrest_user.service.payment.TossClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * demo 프로필(Render 무료 데모)이 외부 서비스 없이 기동하고, 시드·SPA 폴백·스텁 배선이 기대대로인지 확인한다.
 * <ul>
 *     <li>e2e 와 공유하는 스텁(토스·메일·인메모리 Redis)은 등록되지만 {@code /e2e/**} 보조 컨트롤러는 없다.</li>
 *     <li>db/demo-seed.sql 이 H2 에 적재되고 데모 계정 비밀번호가 문서 값과 맞다.</li>
 *     <li>확장자 없는 클라이언트 라우트는 /index.html 로 forward, 정적 파일·API 경로는 forward 하지 않는다.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DisplayName("demo 프로필: 외부 서비스 없이 기동하고 SPA 폴백·데모 시드·스텁 배선이 적용된다")
class DemoProfileStartupTest {

    @Autowired private ApplicationContext context;
    @Autowired private Environment env;
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("스텁은 e2e 와 공유하고, /e2e/** 보조 컨트롤러는 등록되지 않는다")
    void stubWiringWithoutE2eEndpoints() {
        assertThat(context.getBean(TossClient.class)).isInstanceOf(E2eTossClient.class);
        assertThat(context.getBeanNamesForType(EmailService.class)).hasSize(1);
        assertThat(AopUtils.getTargetClass(context.getBean(EmailService.class))).isEqualTo(E2eNoOpEmailService.class);
        assertThat(context.getBeanNamesForType(E2eSupportController.class)).isEmpty();
    }

    @Test
    @DisplayName("Render 용 설정: PORT 수신, 정적 파일 위치, 기존 정규식 SPA 포워딩 끔")
    void renderSettings() {
        assertThat(env.getProperty("server.port")).isEqualTo("8080");
        assertThat(env.getProperty("spring.web.resources.static-locations")).startsWith("file:");
        assertThat(env.getProperty("app.web.legacy-spa-forward.enabled")).isEqualTo("false");
        assertThat(env.getProperty("spring.flyway.enabled")).isEqualTo("false");
    }

    @Test
    @DisplayName("데모 시드: 데모 계정(demo@honeyrest.app / Demo1234!)과 숙소 10곳")
    void demoSeedLoaded() throws Exception {
        User demo = userRepository.findByEmail("demo@honeyrest.app").orElseThrow();
        assertThat(demo.getIsVerified()).isTrue();
        assertThat(passwordEncoder.matches("Demo1234!", demo.getPasswordHash())).isTrue();

        mockMvc.perform(get("/api/accommodations/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        mockMvc.perform(get("/api/accommodations/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("허니레스트 북촌 한옥"));
    }

    @Test
    @DisplayName("클라이언트 라우트(확장자 없음)는 /index.html 로 forward 한다")
    void spaRoutesForwardToIndex() throws Exception {
        for (String path : new String[]{"/", "/login", "/user/mypage", "/user/mypage/reservations", "/accommodations/2",
                "/login/kakao/callback", "/e2e/verification-token"}) {
            mockMvc.perform(get(path)).andExpect(forwardedUrl("/index.html"));
        }
    }

    @Test
    @DisplayName("정적 파일·API·업로드 경로는 SPA 로 넘기지 않는다")
    void serverPathsAreNotForwarded() throws Exception {
        mockMvc.perform(get("/uploads/placeholder/stay-1.svg")).andExpect(forwardedUrl(null));
        mockMvc.perform(get("/assets/index.js")).andExpect(forwardedUrl(null));
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(forwardedUrl(null));
        assertThat(DemoSpaFallbackController.isServerPath("/api")).isTrue();
        assertThat(DemoSpaFallbackController.isServerPath("/api/unknown")).isTrue();
        assertThat(DemoSpaFallbackController.isServerPath("/oauth2/authorization/google")).isTrue();
        assertThat(DemoSpaFallbackController.isServerPath("/apartment")).isFalse();
        assertThat(DemoSpaFallbackController.isServerPath("/user/mypage")).isFalse();
    }

    @Test
    @DisplayName("날씨: API 키가 없으면 예시 날씨(sample=true)를 돌려준다, 모르는 도시는 404")
    void weatherFallsBackToSample() throws Exception {
        mockMvc.perform(get("/api/weather").param("city", "Seoul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("서울"))
                .andExpect(jsonPath("$.sample").value(true))
                .andExpect(jsonPath("$.temp").isNumber())
                .andExpect(jsonPath("$.humidity").isNumber());
        mockMvc.perform(get("/api/weather").param("lat", "33.5").param("lon", "126.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("제주"));
        mockMvc.perform(get("/api/weather").param("city", "Atlantis")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/weather")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("데모 시드의 이미지 경로는 모두 uploads/demo 에 실제 파일이 있다 (플레이스홀더 문구 이미지 미사용)")
    void seededImagesExist() throws Exception {
        List<String> urls = jdbcTemplate.queryForList("""
                SELECT image_url FROM region WHERE image_url IS NOT NULL
                UNION SELECT thumbnail FROM accommodation
                UNION SELECT image_url FROM accommodation_image
                UNION SELECT image_url FROM room_image
                UNION SELECT image_url FROM banner
                UNION SELECT image_url FROM event
                """, String.class);
        assertThat(urls).isNotEmpty().allSatisfy(url -> {
            assertThat(url).startsWith("/uploads/demo/");
            assertThat(Files.exists(Path.of("uploads").resolve(url.substring("/uploads/".length())))).as(url).isTrue();
        });
        mockMvc.perform(get("/uploads/demo/hero-namsan.jpg")).andExpect(status().isOk());
    }
}
