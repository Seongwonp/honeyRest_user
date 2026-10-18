package com.honeyrest.honeyrest_user.e2e;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.service.email.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * e2e·demo 프로필 전용: 메일을 보내지 않는 {@link EmailService} 구현.
 * <p>
 * SMTP·메일 비활성화 구현은 {@code @Profile("!e2e & !demo")} 라 e2e·demo 에서는 이 빈만 등록된다.
 * (demo 프로필에는 토큰 조회 엔드포인트가 없으므로 데모 사이트에서는 신규 가입의 이메일 인증을 마칠 수 없다 → 데모 계정 사용) 인증 링크 토큰은 DB 에 그대로 저장되므로
 * 테스트는 {@code GET /e2e/verification-token?email=} 로 토큰을 받아 가입 인증을 마친다.
 * SMTP 연결을 전혀 시도하지 않으며, 로그에도 토큰/링크를 남기지 않는다.
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
