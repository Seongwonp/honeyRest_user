package com.honeyrest.honeyrest_user.controller.passwordReset;

import com.honeyrest.honeyrest_user.service.PasswordResetService;
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
    public ResponseEntity<?> confirmReset(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        passwordResetService.resetPassword(token, newPassword);
        log.info("🔐 비밀번호 재설정 완료: token={}", token);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다");
    }
}