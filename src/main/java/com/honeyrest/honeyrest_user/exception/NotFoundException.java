package com.honeyrest.honeyrest_user.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 리소스(사용자·예약·리뷰 등)가 없을 때 404 로 응답하기 위한 예외.
 * <p>
 * 과거에는 "찾을 수 없음"을 IllegalArgumentException(→400) 이나 RuntimeException(→500) 으로 던져
 * 클라이언트가 잘못된 입력/서버 오류와 구분할 수 없었다. {@link GlobalExceptionHandler} 가 {@link ApiException} 으로 처리한다.
 * <p>
 * 로그인 실패(이메일/비밀번호 불일치)처럼 존재 여부를 숨겨야 하는 경우에는 쓰지 않는다.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
