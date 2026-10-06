package com.honeyrest.honeyrest_user.config;

import com.honeyrest.honeyrest_user.security.CustomOAuth2UserService;
import com.honeyrest.honeyrest_user.security.JwtTokenProvider;
import com.honeyrest.honeyrest_user.filter.JwtAuthenticationFilter;
import com.honeyrest.honeyrest_user.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/verify-password").authenticated()
                        // 이메일 변경 확정은 본인 계정에만 영향을 줘야 하므로 인증이 필요하다(P0-8).
                        // 과거에는 /api/user/email/** 전체가 permitAll이라 인증 없이도 호출할 수 있었다.
                        .requestMatchers("/api/user/email/verify-change").authenticated()
                        // 숙소/이벤트/배너 목록·상세 조회(GET)는 비회원도 볼 수 있어야 하므로 공개한다.
                        .requestMatchers(HttpMethod.GET, "/api/accommodations/**", "/api/event/**", "/api/banner/**")
                        .permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/password-reset/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/user/email/**",
                                "/api/weather/**",
                                "/api/region/**",
                                "/api/room/**",
                                "/api/reserve/form-info",
                                "/api/reserve/guest-lookup",
                                "/swagger-resources/**",
                                "/webjars/**",
                                // 로컬 파일 저장소(app.storage.type=local)의 업로드 이미지는 공개 조회 가능해야 한다.
                                "/uploads/**"
                        ).permitAll()
                        // 배너/이벤트 등록과 숙소 태그 매핑 변경(POST/PUT/DELETE)은 관리자 전용이다.
                        // 과거에는 이 경로 전체가 permitAll이라 인증 없이 누구나 배너·이벤트를 생성하거나
                        // 숙소 태그를 조작할 수 있었다(P0-3). 이 백엔드에는 아직 ADMIN 역할을 실제로
                        // 부여하는 절차가 없으므로, 당장은 사실상 전면 차단되며 이는 의도된 fail-closed다.
                        .requestMatchers("/api/accommodations/**", "/api/event/**", "/api/banner/**")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                "/api/user/**",
                                "/api/wishList/**",
                                "/api/review/**",
                                "/api/payment/**",
                                "/api/files/**"
                        ).authenticated()
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().permitAll()
                )
                // /api/**는 브라우저 로그인 페이지가 아니라 JSON API이므로, 인증/인가 실패 시
                // oauth2Login의 loginPage("/login")로 리다이렉트되지 않고 바로 401/403을 반환해야 한다.
                // 과거에는 리다이렉트를 따라가다 존재하지 않는 정적 리소스를 찾으며 500으로 귀결됐다.
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**"))
                        .defaultAccessDeniedHandlerFor(
                                new AccessDeniedHandlerImpl(),
                                new AntPathRequestMatcher("/api/**"))
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler) // JWT 발급 핸들러
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 허용 Origin 목록 (쉼표 구분). 운영(prod)에서는 APP_CORS_ORIGINS 환경변수로 정한다.
     * 단일 서버 배포는 SPA 와 API 가 같은 Origin(Caddy 뒤)이라 CORS 가 사실상 쓰이지 않지만,
     * 프론트를 다른 Origin 에 둘 때를 대비해 코드 수정 없이 바꿀 수 있게 한다.
     */
    @Value("${app.cors.allowed-origins:http://localhost:5173,https://honeyrest-user-react.vercel.app}")
    private String corsAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(parseOrigins(corsAllowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** "a, b,,c/" → [a, b, c] (공백·빈 값 제거, 끝의 / 제거 — Origin 헤더에는 경로가 붙지 않는다) */
    static List<String> parseOrigins(String raw) {
        if (raw == null) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(origin -> origin.replaceAll("/+$", ""))
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList();
    }




    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}