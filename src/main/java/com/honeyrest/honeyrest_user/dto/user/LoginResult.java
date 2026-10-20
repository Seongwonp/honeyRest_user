package com.honeyrest.honeyrest_user.dto.user;

/**
 * 로그인(일반/소셜) 서비스 결과. 응답 본문과 HttpOnly 쿠키로 내릴 refresh token 을 함께 담는다.
 * 쿠키는 웹 계층이 만들고, refresh token 은 응답 본문에 노출하지 않는다.
 */
public record LoginResult(LoginResponseDTO response, String refreshToken) {

    @Override
    public String toString() {
        // 로그에 토큰이 남지 않도록 refresh token 은 가린다.
        return "LoginResult[user=" + (response != null && response.getUser() != null ? response.getUser().getUserId() : null) + "]";
    }
}
