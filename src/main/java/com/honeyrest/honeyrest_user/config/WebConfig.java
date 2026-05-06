package com.honeyrest.honeyrest_user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String FORWARD_INDEX = "forward:/index.html";


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

    // 정적 리소스 핸들링 (선택)
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}