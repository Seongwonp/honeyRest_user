package com.honeyrest.honeyrest_user.controller.auth;

import com.honeyrest.honeyrest_user.dto.user.*;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.security.google.GoogleOAuthService;
import com.honeyrest.honeyrest_user.security.kakao.KakaoOAuthService;
import com.honeyrest.honeyrest_user.service.RefreshTokenService;
import com.honeyrest.honeyrest_user.service.UserService;
import com.honeyrest.honeyrest_user.util.RefreshTokenCookieManager;
import jakarta.servlet.http.HttpServletResponse;
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
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @PostMapping("/signup")
    public UserResponseDTO signup(@ModelAttribute UserSignupRequestDTO request) throws Exception {
        return userService.signup(request);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody UserLoginRequestDTO request, HttpServletResponse response) {
        return userService.login(request, response);
    }

    @PostMapping("/social-login")
    public LoginResponseDTO socialLogin(@RequestBody SocialLoginRequestDTO request, HttpServletResponse response) {
        return userService.socialLogin(request, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal CustomUserPrincipal principal, HttpServletResponse response) {
        // RefreshToken 쿠키 삭제는 무조건 수행
        response.addHeader("Set-Cookie", refreshTokenCookieManager.clear().toString());

        // 인증 객체가 있을 경우만 DB에서 RefreshToken 삭제
        if (principal != null) {
            refreshTokenService.invalidateAllByUser(principal.getUser());
            log.info("🚪 로그아웃 처리 완료: userId={}", principal.getUser().getUserId());
        } else {
            log.warn("❌ 인증 정보 없음 → 쿠키만 삭제됨");
        }

        return ResponseEntity.ok("로그아웃 처리 완료");
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<?> kakaoCallback(@RequestParam String code, HttpServletResponse response){
        String accessToken = kakaoOAuthService.getKakaoAccessToken(code);
        SocialLoginRequestDTO userInfo = kakaoOAuthService.getUserInfo(accessToken);

        SocialLoginRequestDTO dto = SocialLoginRequestDTO.builder()
                .socialType("KAKAO")
                .socialId(userInfo.getSocialId())
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .profileImage(userInfo.getProfileImage())
                .build();

        LoginResponseDTO loginResponse = userService.socialLogin(dto,response);
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam String code, HttpServletResponse response) {
        String accessToken = googleOAuthService.getGoogleAccessToken(code);
        SocialLoginRequestDTO userInfo = googleOAuthService.getUserInfo(accessToken);

        SocialLoginRequestDTO dto = SocialLoginRequestDTO.builder()
                .socialType("GOOGLE")
                .socialId(userInfo.getSocialId())
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .profileImage(userInfo.getProfileImage())
                .build();

        LoginResponseDTO loginResponse = userService.socialLogin(dto,response);
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


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            log.warn("❌ RefreshToken 쿠키 없음 → 재발급 불가");
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        log.info("🔄 Refresh 요청 수신 (쿠키 기반)");
        try {
            String newAccessToken = refreshTokenService.validateAndReissue(refreshToken);
            return ResponseEntity.ok(newAccessToken);
        } catch (RuntimeException e) {
            log.warn("❌ Refresh 실패: {}", e.getMessage());
            return ResponseEntity.status(401).body("토큰이 유효하지 않거나 만료되었습니다.");
        }
    }
}