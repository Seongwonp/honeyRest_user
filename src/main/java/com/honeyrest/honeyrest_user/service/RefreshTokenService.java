package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.entity.RefreshToken;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.RefreshTokenRepository;
import com.honeyrest.honeyrest_user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Log4j2
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public void saveRefreshToken(User user, String tokenValue, LocalDateTime expiryDate) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenValue(tokenValue)
                .expiryDate(expiryDate)
                .build();

        refreshTokenRepository.save(token);
        log.info("💾 RefreshToken 저장 완료: userId={}", user.getUserId());
    }

    public String validateAndReissue(String refreshTokenValue) {
        RefreshToken token = refreshTokenRepository.findByTokenValue(refreshTokenValue)
                .orElseThrow(() -> {
                    log.warn("❌ RefreshToken 조회 실패");
                    return new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
                });

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("⏳ RefreshToken 만료됨: {}", token.getExpiryDate());
            refreshTokenRepository.delete(token);
            throw new RuntimeException("리프레시 토큰이 만료되었습니다.");
        }

        User user = token.getUser();
        String newAccessToken = jwtTokenProvider.reissueAccessToken(user);
        log.info("✅ AccessToken 재발급 성공: userId={}", user.getUserId());

        return newAccessToken;
    }

    @Transactional
    public void invalidateAllByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
        log.info("🚪 모든 RefreshToken 삭제 완료: userId={}", user.getUserId());
    }


}
