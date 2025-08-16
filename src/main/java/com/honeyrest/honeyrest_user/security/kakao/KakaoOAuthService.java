package com.honeyrest.honeyrest_user.security.kakao;

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
public class KakaoOAuthService {

    private final RestTemplate restTemplate;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    public String getKakaoAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("access_token")) {
            log.error("카카오 access token 발급 실패: {}", responseBody);
            throw new RuntimeException("카카오 access token 발급 실패");
        }

        String accessToken = (String) responseBody.get("access_token");
        log.info("카카오 access token 발급 성공: {}", accessToken);
        return accessToken;
    }

    public SocialLoginRequestDTO getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("id")) {
            log.error("카카오 사용자 정보 조회 실패: {}", response);
            throw new RuntimeException("카카오 사용자 정보 조회 실패");
        }

        String id = String.valueOf(body.get("id"));

        Object accountObj = body.get("kakao_account");
        if (!(accountObj instanceof Map)) {
            log.error("카카오 계정 정보가 없습니다: {}", accountObj);
            throw new RuntimeException("카카오 계정 정보가 없습니다.");
        }

        Map<String, Object> kakaoAccount = (Map<String, Object>) accountObj;
        String email = kakaoAccount.get("email") instanceof String ? (String) kakaoAccount.get("email") : null;

        Object profileObj = kakaoAccount.get("profile");
        Map<String, Object> profile = profileObj instanceof Map ? (Map<String, Object>) profileObj : null;

        String nickname = profile != null && profile.get("nickname") instanceof String
                ? (String) profile.get("nickname") : "카카오사용자";

        String profileImage = profile != null && profile.get("profile_image_url") instanceof String
                ? (String) profile.get("profile_image_url") : null;

        log.info("카카오 사용자 정보 조회 성공");
        log.info("ID: {}, Email: {}, Nickname: {}", id, email, nickname);

        return SocialLoginRequestDTO.builder()
                .socialType("KAKAO")
                .socialId(id)
                .email(email)
                .name(nickname)
                .profileImage(profileImage)
                .build();
    }
}