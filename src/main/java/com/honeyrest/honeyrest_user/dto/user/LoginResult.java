package com.honeyrest.honeyrest_user.dto.user;

/**
 * 로그인(일반/소셜) 서비스 결과.
 * <p>
 * 응답 본문({@link LoginResponseDTO}: 사용자 정보 + access token)과 HttpOnly 쿠키로 내려야 할 refresh token 을 함께 담는다.
 * 쿠키를 만드는 일은 웹 계층(컨트롤러 + {@code RefreshTokenCookieManager})의 책임이라 서비스는
 * {@code HttpServletResponse} 를 받지 않고 이 값만 돌려준다.
 * refresh token 은 응답 본문에 노출하지 않는다.
 */
public record LoginResult(LoginResponseDTO response, String refreshToken) {

    @Override
    public String toString() {
        // 로그에 토큰이 남지 않도록 refresh token 은 가린다.
        return "LoginResult[user=" + (response != null && response.getUser() != null ? response.getUser().getUserId() : null) + "]";
    }
}
