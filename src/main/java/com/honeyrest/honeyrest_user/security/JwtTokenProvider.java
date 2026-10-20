package com.honeyrest.honeyrest_user.security;

import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final UserRepository userRepository;

    public JwtTokenProvider(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${jwt.access-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration,
            UserRepository userRepository
    ) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.userRepository = userRepository;
    }

    // Access Token 생성 (토큰 폐기 이력이 없는 경우)
    public String createAccessToken(Long userId, String role) {
        return createAccessToken(userId, role, null);
    }

    /**
     * 사용자 기준 Access Token 생성. tokenValidAfter 가 있는 사용자는 반드시 이 메서드로 발급한다.
     */
    public String createAccessToken(User user) {
        return createAccessToken(user.getUserId(), user.getRole(), user.getTokenValidAfter());
    }

    /**
     * iat = max(현재 시각, tokenValidAfter) 로 발급한다. 폐기 직후 같은 초에 재발급해도 통과하고 이전 토큰은 거부된다.
     */
    private String createAccessToken(Long userId, String role, LocalDateTime tokenValidAfter) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role);

        Date now = new Date();
        if (tokenValidAfter != null) {
            Date validAfter = Date.from(tokenValidAfter.atZone(ZoneId.systemDefault()).toInstant());
            if (validAfter.after(now)) {
                now = validAfter;
            }
        }
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.info("✅ AccessToken 생성 완료: userId={}, role={}, 만료={}", userId, role, expiry);
        return token;
    }

    // Refresh Token 생성 (UUID 기반)
    public String createRefreshToken() {
        String token = UUID.randomUUID().toString();
        log.info("✅ RefreshToken 생성 완료");
        return token;
    }

    // AccessToken 재발급 (RefreshToken 검증 후)
    public String reissueAccessToken(User user) {
        String newToken = createAccessToken(user);
        log.info("🔄 AccessToken 재발급 완료: userId={}, role={}", user.getUserId(), user.getRole());
        return newToken;
    }
    // AccessToken 유효성 검사
    public void validateTokenOrThrow(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            Date expiration = claims.getBody().getExpiration();
            if (expiration.before(new Date())) {
                log.warn("❌ AccessToken 만료됨: {}", expiration);
                throw new JwtException("토큰이 만료되었습니다.");
            }

            log.info("✅ AccessToken 유효성 검사 통과");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("❌ AccessToken 유효성 검사 실패: {}", e.getMessage());
            throw new JwtException("유효하지 않은 토큰입니다.");
        }
    }

    // AccessToken에서 userId 추출
    public Long getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long userId = Long.parseLong(claims.getSubject());
        log.info("🔍 AccessToken에서 추출한 userId: {}", userId);
        return userId;
    }

    // Authorization 헤더에서 AccessToken 추출
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("📦 Authorization 헤더에서 AccessToken 추출 완료");
            return token;
        }
        // 비로그인 요청마다 생기는 정상 상황이라 DEBUG 로 남긴다
        log.debug("❌ Authorization 헤더 없음 또는 형식 오류");
        return null;
    }

    // 인증 객체 생성
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long userId = Long.parseLong(claims.getSubject());
        Date issuedAt = claims.getIssuedAt();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ 사용자 정보 조회 실패: userId={}", userId);
                    return new RuntimeException("User not found");
                });

        // 폐기 시각(tokenValidAfter)보다 먼저 발급된 토큰은 거부한다. iat 가 초 단위라 epoch 초로 비교한다.
        if (user.getTokenValidAfter() != null && issuedAt != null
                && isIssuedBefore(issuedAt, user.getTokenValidAfter())) {
            log.warn("❌ 폐기된 토큰: userId={}, issuedAt={}", userId, issuedAt);
            throw new JwtException("폐기된 토큰입니다. 다시 로그인해주세요.");
        }
        if ("DELETED".equals(user.getStatus())) {
            log.warn("❌ 탈퇴한 계정의 토큰: userId={}", userId);
            throw new JwtException("탈퇴한 계정입니다.");
        }

        CustomUserPrincipal principal = new CustomUserPrincipal(user, Map.of());
        log.info("✅ 인증 객체 생성 완료: userId={}, role={}", user.getUserId(), user.getRole());

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    /** iat(초 단위)가 tokenValidAfter(초 단위로 내림)보다 이전이면 true. */
    static boolean isIssuedBefore(Date issuedAt, LocalDateTime tokenValidAfter) {
        long iatSeconds = issuedAt.toInstant().getEpochSecond();
        long validAfterSeconds = tokenValidAfter.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        return iatSeconds < validAfterSeconds;
    }
}
