package com.honeyrest.honeyrest_user.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 리소스가 없을 때 404 로 응답하기 위한 예외.
 * 로그인 실패처럼 존재 여부를 숨겨야 하는 경우에는 쓰지 않는다.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
