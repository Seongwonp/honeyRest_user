package com.honeyrest.honeyrest_user.controller.passwordReset;

import com.honeyrest.honeyrest_user.service.PasswordResetService;
import com.honeyrest.honeyrest_user.util.RefreshTokenCookieManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    // 비밀번호 초기화 요청
    @PostMapping("/request")
    public ResponseEntity<?> requestReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        passwordResetService.requestReset(email);
        log.info("🔐 비밀번호 초기화 요청 처리 완료: {}", email);
        return ResponseEntity.ok("비밀번호 초기화 이메일이 발송되었습니다");
    }

    // 비밀번호 재설정
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmReset(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        passwordResetService.resetPassword(token, newPassword);
        // DB의 refresh token은 서비스에서 삭제했으므로 이 브라우저의 쿠키도 함께 지운다.
        response.addHeader("Set-Cookie", refreshTokenCookieManager.clear().toString());
        log.info("🔐 비밀번호 재설정 완료");
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다");
    }
}