package com.honeyrest.honeyrest_user.service.email;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;

/**
 * 메일 발송 포트. 호출자(결제·비밀번호 재설정·이메일 인증)는 이 인터페이스에만 의존한다.
 * <p>
 * 구현은 프로필/조건에 따라 정확히 하나만 빈으로 등록된다.
 * <ul>
 *   <li>{@link SmtpEmailService}: e2e 가 아니고 메일 설정이 있을 때 ({@link MailEnabledCondition})</li>
 *   <li>{@link NoOpEmailService}: e2e 가 아니고 메일 설정이 없을 때 ({@link MailEnabledCondition.Disabled})</li>
 *   <li>{@code e2e.E2eNoOpEmailService}: e2e 프로필</li>
 * </ul>
 * 과거에는 no-op 구현이 SMTP 구현 클래스를 상속하고 {@code super(null)} 로 JavaMailSender 를 비워 두었다.
 * 재정의를 빠뜨리면 null 로 발송을 시도하는 구조(LSP 위반)여서 인터페이스로 분리했다.
 * <p>
 * 모든 메서드는 실패해도 예외를 호출자에게 전파하지 않는다(발송 실패가 예약·가입 흐름을 깨지 않도록).
 */
public interface EmailService {

    /** 예약 완료 안내 메일. */
    void sendReservationConfirmation(String email, ReservationCompleteDTO dto);

    /** 비밀번호 재설정 링크 메일. tokenValue 로 링크를 만든다. */
    void sendPasswordReset(String email, String tokenValue);

    /** 가입 이메일 인증 메일. link 는 완성된 인증 링크. */
    void sendVerificationEmail(String email, String link);

    /** 이메일 변경 인증 메일. link 는 완성된 인증 링크. */
    void sendEmailChangeToken(String email, String link);
}
