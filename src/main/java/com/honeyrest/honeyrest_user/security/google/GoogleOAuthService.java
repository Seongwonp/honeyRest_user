package com.honeyrest.honeyrest_user.security.google;

import com.honeyrest.honeyrest_user.dto.user.SocialLoginRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@Log4j2
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final RestTemplate restTemplate;

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.client-secret}")
    private String googleClientSecret;

    @Value("${app.base-url}")
    private String baseUrl;

    public String getGoogleAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", baseUrl + "/login/google/callback");
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
        );

        String accessToken = (String) response.getBody().get("access_token");
        if (accessToken == null) {
            log.error("구글 access token 발급 실패: {}", response.getBody());
            throw new RuntimeException("구글 access token 발급 실패");
        }

        log.info("구글 access token 발급 성공: {}", accessToken);
        return accessToken;
    }

    public SocialLoginRequestDTO getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("sub")) {
            log.error("구글 사용자 정보 조회 실패: {}", response);
            throw new RuntimeException("구글 사용자 정보 조회 실패");
        }

        String id = (String) body.get("sub");
        String email = (String) body.get("email");
        String name = (String) body.get("name");
        String profileImage = (String) body.get("picture");

        log.info("구글 사용자 정보 조회 성공: ID={}, Email={}, Name={}", id, email, name);

        return SocialLoginRequestDTO.builder()
                .socialType("GOOGLE")
                .socialId(id)
                .email(email)
                .name(name)
                .profileImage(profileImage)
                .build();
    }

}