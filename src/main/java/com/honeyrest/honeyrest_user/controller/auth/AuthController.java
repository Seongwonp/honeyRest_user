package com.honeyrest.honeyrest_user.controller.auth;

import com.honeyrest.honeyrest_user.dto.user.*;
import com.honeyrest.honeyrest_user.security.google.GoogleOAuthService;
import com.honeyrest.honeyrest_user.security.kakao.KakaoOAuthService;
import com.honeyrest.honeyrest_user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final KakaoOAuthService kakaoOAuthService;
    private final GoogleOAuthService googleOAuthService;

    @PostMapping("/signup")
    public UserResponseDTO signup(@ModelAttribute UserSignupRequestDTO request) throws Exception {
        return userService.signup(request);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody UserLoginRequestDTO request) {
        return userService.login(request);
    }

    @PostMapping("/social-login")
    public LoginResponseDTO socialLogin(@RequestBody SocialLoginRequestDTO request) {
        return userService.socialLogin(request);
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<?> kakaoCallback(@RequestParam String code) {
        String accessToken = kakaoOAuthService.getKakaoAccessToken(code);
        SocialLoginRequestDTO userInfo = kakaoOAuthService.getUserInfo(accessToken);

        SocialLoginRequestDTO dto = SocialLoginRequestDTO.builder()
                .socialType("KAKAO")
                .socialId(userInfo.getSocialId())
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .profileImage(userInfo.getProfileImage())
                .build();

        LoginResponseDTO loginResponse = userService.socialLogin(dto);
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam String code) {
        String accessToken = googleOAuthService.getGoogleAccessToken(code);
        SocialLoginRequestDTO userInfo = googleOAuthService.getUserInfo(accessToken);

        SocialLoginRequestDTO dto = SocialLoginRequestDTO.builder()
                .socialType("GOOGLE")
                .socialId(userInfo.getSocialId())
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .profileImage(userInfo.getProfileImage())
                .build();

        LoginResponseDTO loginResponse = userService.socialLogin(dto);
        return ResponseEntity.ok(loginResponse);
    }

}