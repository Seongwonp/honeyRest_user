package com.honeyrest.honeyrest_user.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieManager {

    private final boolean secure;
    private final String sameSite;
    private final long maxAgeSeconds;
    private final String path;

    public RefreshTokenCookieManager(
            @Value("${security.cookie.refresh.secure:false}") boolean secure,
            @Value("${security.cookie.refresh.same-site:Strict}") String sameSite,
            @Value("${security.cookie.refresh.max-age-seconds:604800}") long maxAgeSeconds,
            @Value("${security.cookie.refresh.path:/}") String path
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAgeSeconds = maxAgeSeconds;
        this.path = path;
    }

    public ResponseCookie create(String tokenValue) {
        return ResponseCookie.from("refreshToken", tokenValue)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }
}

