package com.honeyrest.honeyrest_user.controller.auth;

import com.honeyrest.honeyrest_user.dto.email.EmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.email.ResendEmailRequestDTO;
import com.honeyrest.honeyrest_user.dto.email.TokenStatusResponseDTO;
import com.honeyrest.honeyrest_user.service.email.EmailVerificationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}