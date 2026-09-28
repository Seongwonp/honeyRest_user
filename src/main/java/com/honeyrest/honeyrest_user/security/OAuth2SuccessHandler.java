package com.honeyrest.honeyrest_user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        String email = userPrincipal.getUser().getEmail();

        // JWT 발급 - subject를 이메일이 아닌 userId로 발급해야 JwtTokenProvider.getUserId()가 정상 동작한다(P1-11).
        // 로그아웃 직후 같은 초에 재로그인해도 거부되지 않도록 User 기준 발급을 사용한다(iat ≥ tokenValidAfter).
        String token = jwtTokenProvider.createAccessToken(userPrincipal.getUser());

        // 응답 JSON 구성
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", token);
        responseBody.put("email", email);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}