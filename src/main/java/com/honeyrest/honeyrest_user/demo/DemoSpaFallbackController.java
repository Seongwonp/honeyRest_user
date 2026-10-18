package com.honeyrest.honeyrest_user.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * demo 프로필 전용 SPA 폴백 — <b>Render 무료 데모(단일 컨테이너)에서만 등록된다.</b>
 * <p>
 * demo 이미지는 React SPA 빌드(dist/)를 {@code /app/static} 에 두고 API 와 같은 Origin 에서 서빙한다.
 * 브라우저가 {@code /user/mypage} 같은 클라이언트 라우트로 직접 들어오거나 새로고침하면 서버에는 그런 파일이 없으므로,
 * 확장자가 없는 GET 요청을 {@code /index.html} 로 forward 해 React Router 가 화면을 그리게 한다.
 * <ul>
 *     <li>매핑 패턴 {@code /**}/{last:[^.]*} 은 마지막 경로 조각에 점(.)이 없을 때만 맞는다 →
 *         {@code /assets/index-xxx.js}, {@code /uploads/placeholder/stay-1.svg}, {@code /favicon.ico} 같은 파일 요청은
 *         이 컨트롤러를 타지 않고 정적 리소스 핸들러로 간다.</li>
 *     <li>API·업로드·OAuth·관리 엔드포인트 접두어는 SPA 로 넘기지 않고 404 로 끝낸다
 *         (없는 API 를 index.html 200 으로 감추지 않기 위해). 실제 매핑된 API 는 더 구체적인 패턴이라 항상 먼저 선택된다.</li>
 *     <li>{@code /actuator/**} 는 별도 핸들러 매핑(우선순위가 더 높음)이 처리하고, OAuth2 로그인 경로는 보안 필터가 먼저 처리한다.</li>
 * </ul>
 * 다른 프로필(운영 Caddy 배포 등)에는 영향이 없다 — 그쪽은 Caddy 가 SPA 폴백을 맡는다.
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
