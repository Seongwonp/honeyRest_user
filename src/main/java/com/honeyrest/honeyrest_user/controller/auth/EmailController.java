package com.honeyrest.honeyrest_user.controller.auth;

import com.honeyrest.honeyrest_user.dto.email.EmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.email.ResendEmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.email.TokenStatusResponseDTO;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.service.email.EmailVerificationTokenService;
import lombok.RequiredArgsConstructor;
import com.honeyrest.honeyrest_user.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailVerificationTokenService emailService;

    // 이메일 인증 링크 전송
    @PostMapping("/send")
    public ResponseEntity<Void> sendVerificationEmail(@RequestBody EmailRequestDTO dto) {
        emailService.sendVerificationEmail(dto);
        return ResponseEntity.ok().build();
    }

    // 이메일 인증 처리
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        emailService.verifyToken(token);
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }

    // 인증 메일 재전송
    @PostMapping("/resend")
    public ResponseEntity<Void> resendVerificationEmail(@RequestBody ResendEmailRequestDTO dto) {
        emailService.resendVerificationEmail(dto);
        return ResponseEntity.ok().build();
    }

    // 토큰 상태 확인
    @GetMapping("/status")
    public ResponseEntity<TokenStatusResponseDTO> getTokenStatus(@RequestParam("token") String token) {
        TokenStatusResponseDTO status = emailService.getTokenStatus(token);
        return ResponseEntity.ok(status);
    }

    // 이메일 변경 인증 처리 (본인 인증 필요 - SecurityConfig에서 이 경로만 별도로 authenticated 처리)
    @PostMapping("/verify-change")
    public ResponseEntity<String> verifyEmailChange(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam("token") String token,
            @RequestParam("newEmail") String newEmail
    ) {
        if (principal == null) {
            // 미인증은 403(AccessDenied)이 아니라 401 이어야 프론트가 토큰 재발급/로그인 흐름을 탄다.
            throw new ApiException("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }
        emailService.confirmEmailChange(principal.getUserId(), token, newEmail);
        return ResponseEntity.ok("이메일 변경이 완료되었습니다.");
    }

}