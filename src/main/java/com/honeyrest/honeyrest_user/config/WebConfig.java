package com.honeyrest.honeyrest_user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String FORWARD_INDEX = "forward:/index.html";

    /** 로컬 파일 저장소 디렉터리 (storage/LocalFileStorage 와 동일한 프로퍼티) */
    @Value("${app.storage.local.dir:./uploads}")
    private String localStorageDir;

    /**
     * SPA 빌드 결과(index.html, assets/*)를 찾을 위치. 기본은 jar 안의 classpath:/static/ 이고,
     * demo 프로필(Render 단일 컨테이너)은 이미지에 복사한 SPA 빌드 디렉터리(file:/app/static/)를 앞에 둔다.
     * 아래 "/**" 핸들러가 Spring Boot 기본 정적 리소스 핸들러를 대신하므로 같은 프로퍼티를 그대로 따른다.
     */
    @Value("${spring.web.resources.static-locations:classpath:/static/}")
    private String[] staticLocations;


    /**
     * 아래 정규식 기반 React 라우팅 포워딩 사용 여부. demo 프로필은 false 로 끄고
     * demo.DemoSpaFallbackController(확장자 없는 GET 만 index.html 로)를 쓴다.
     * 이 view controller 들은 정적 리소스 핸들러보다 먼저 매칭되어 /assets/*.css·*.js 요청까지 index.html 로 보내므로,
     * SPA 정적 파일을 이 서버가 직접 서빙하는 demo 에서는 쓸 수 없다. (운영은 Caddy 가 SPA 를 서빙해 영향 없음)
     */
    @Value("${app.web.legacy-spa-forward.enabled:true}")
    private boolean legacySpaForwardEnabled;

    // React 라우팅 포워딩 설정
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        if (!legacySpaForwardEnabled) {
            return;
        }
        registry.addViewController("/{spring:\\w+}")
                .setViewName(FORWARD_INDEX);
        registry.addViewController("/**/{spring:\\w+}")
                .setViewName(FORWARD_INDEX);
        registry.addViewController("/{spring:\\w+}/**{spring:?!(\\.js|\\.css)$}")
                .setViewName(FORWARD_INDEX);
    }

    // 정적 리소스 핸들링
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 로컬 저장소 업로드 파일 / 플레이스홀더 이미지 서빙 (/uploads/{folder}/{file})
        String location = Paths.get(localStorageDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations(location);

        registry
                .addResourceHandler("/**")
                .addResourceLocations(staticLocations);
    }
}
