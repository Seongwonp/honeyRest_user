package com.honeyrest.honeyrest_user.service.email;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 메일 설정이 없을 때 쓰는 "발송하지 않는" {@link EmailService} 구현 ({@link MailEnabledCondition} 이 false 일 때).
 * 받는 사람 주소·토큰·링크는 로그에 남기지 않는다. 메일이 꺼져 있으면 일반 가입의 이메일 인증을 마칠 수 없다.
 */
@Log4j2
@Service
@Profile("!e2e & !demo")
@Conditional(MailEnabledCondition.Disabled.class)
public class NoOpEmailService implements EmailService {

    @Override
    public void sendReservationConfirmation(String email, ReservationCompleteDTO dto) {
        log.warn("[mail-disabled] 예약 완료 메일 발송 생략 (메일 설정 없음)");
    }

    @Override
    public void sendPasswordReset(String email, String tokenValue) {
        log.warn("[mail-disabled] 비밀번호 재설정 메일 발송 생략 (메일 설정 없음)");
    }

    @Override
    public void sendVerificationEmail(String email, String link) {
        log.warn("[mail-disabled] 가입 인증 메일 발송 생략 (메일 설정 없음)");
    }

    @Override
    public void sendEmailChangeToken(String email, String link) {
        log.warn("[mail-disabled] 이메일 변경 인증 메일 발송 생략 (메일 설정 없음)");
    }
}
