package com.honeyrest.honeyrest_user.service.email;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 메일 설정이 없을 때 쓰는 "발송하지 않는" {@link EmailService}.
 * <p>
 * {@link MailEnabledCondition} 이 false 일 때(예: 운영 배포에서 MAIL_USERNAME 을 비워 둔 경우)만 등록된다.
 * SMTP 연결을 시도하지 않으며 받는 사람 주소·토큰·링크는 로그에 남기지 않는다.
 * e2e 프로필은 별도의 {@code e2e.E2eNoOpEmailService} 를 쓰므로 여기서 제외한다.
 * <p>
 * 주의: 메일이 꺼져 있으면 이메일 인증 링크가 발송되지 않아 일반 회원가입의 인증 단계를 마칠 수 없다.
 * 공개 데모라면 Gmail 앱 비밀번호를 설정하는 것을 권장한다 (docs/DEPLOY.md 참고).
 */
@Log4j2
@Service
@Profile("!e2e")
@Conditional(MailEnabledCondition.Disabled.class)
public class NoOpEmailService extends EmailService {

    public NoOpEmailService() {
        // 부모의 JavaMailSender 는 쓰지 않는다 (모든 발송 메서드를 재정의).
        super(null);
    }

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
