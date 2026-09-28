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


    // React 라우팅 포워딩 설정
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
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
                .addResourceLocations("classpath:/static/");
    }
}
