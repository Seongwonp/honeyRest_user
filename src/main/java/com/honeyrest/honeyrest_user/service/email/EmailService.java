package com.honeyrest.honeyrest_user.service.email;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;

/**
 * 메일 발송 포트. 구현은 프로필/조건에 따라 하나만 등록된다
 * ({@link SmtpEmailService}, {@link NoOpEmailService}, e2e·demo 는 {@code e2e.E2eNoOpEmailService}).
 * 발송 실패가 예약·가입 흐름을 깨지 않도록 예외를 호출자에게 전파하지 않는다.
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
