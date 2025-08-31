package com.honeyrest.honeyrest_user.controller.auth;

import com.honeyrest.honeyrest_user.dto.user.*;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.security.google.GoogleOAuthService;
import com.honeyrest.honeyrest_user.security.kakao.KakaoOAuthService;
import com.honeyrest.honeyrest_user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Log4j2
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

    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(
            @RequestBody PasswordRequestDTO request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        boolean verified = userService.verifyPassword(principal.getUserId(), request.getPassword());

        if (!verified) {
            log.info("❌ 비밀번호 불일치");
            return ResponseEntity.status(400).body("비밀번호가 일치하지 않습니다.");
        }

        log.info("✅ 비밀번호 인증 성공");
        return ResponseEntity.ok().build();
    }

}