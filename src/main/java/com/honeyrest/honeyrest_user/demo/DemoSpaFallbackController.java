package com.honeyrest.honeyrest_user.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * demo 프로필 전용 SPA 폴백. 확장자가 없는 GET 요청을 {@code /index.html} 로 forward 해 React Router 가 처리하게 한다.
 * <p>
 * 마지막 경로 조각에 점(.)이 있으면 정적 리소스 핸들러로 간다. API·업로드·OAuth 등 서버 경로 접두어는
 * 없는 API 를 index.html 200 으로 감추지 않도록 404 로 끝낸다. 운영은 Caddy 가 SPA 폴백을 맡는다.
 */
@Controller
@Profile("demo")
public class DemoSpaFallbackController {

    /** SPA 로 넘기지 않는 서버 경로 접두어 */
    static final List<String> SERVER_PREFIXES = List.of(
            "/api/", "/api-docs", "/uploads/", "/oauth2/", "/login/oauth2/",
            "/actuator", "/swagger-ui", "/v3/api-docs", "/error"
    );

    @GetMapping({"/", "/**/{last:[^.]*}"})
    public String forwardToIndex(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isServerPath(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }

    static boolean isServerPath(String path) {
        if ("/api".equals(path)) {
            return true;
        }
        return SERVER_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
