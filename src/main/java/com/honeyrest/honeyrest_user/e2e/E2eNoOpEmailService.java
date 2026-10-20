package com.honeyrest.honeyrest_user.e2e;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.service.email.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * e2e·demo 프로필 전용: 메일을 보내지 않는 {@link EmailService} 구현.
 * 인증 토큰은 DB 에 저장되므로 테스트는 {@code GET /e2e/verification-token?email=} 로 토큰을 받는다.
 * 로그에도 토큰/링크를 남기지 않는다.
 */
@Log4j2
@Service
@Profile({"e2e", "demo"})
public class E2eNoOpEmailService implements EmailService {

    @Override
    public void sendReservationConfirmation(String email, ReservationCompleteDTO dto) {
        log.info("[e2e] 예약 완료 메일 발송 생략");
    }

    @Override
    public void sendPasswordReset(String email, String tokenValue) {
        log.info("[e2e] 비밀번호 재설정 메일 발송 생략");
    }

    @Override
    public void sendVerificationEmail(String email, String link) {
        log.info("[e2e] 가입 인증 메일 발송 생략");
    }

    @Override
    public void sendEmailChangeToken(String email, String link) {
        log.info("[e2e] 이메일 변경 인증 메일 발송 생략");
    }
}
