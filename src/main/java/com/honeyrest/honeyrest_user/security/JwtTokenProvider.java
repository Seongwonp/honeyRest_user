package com.honeyrest.honeyrest_user.security;

import com.honeyrest.honeyrest_user.entity.User;
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
import java.util.Date;
import java.util.Map;

@Log4j2
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long expirationTime;
    private final UserRepository userRepository;

    public JwtTokenProvider(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${jwt.expiration}") long expirationTime,
            UserRepository userRepository
    ) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.expirationTime = expirationTime;
        this.userRepository = userRepository;
    }

    public String createToken(Long userId, String role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationTime);

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.info("✅ JWT 생성 완료: userId={}, role={}, 만료={}", userId, role, expiry);
        return token;
    }

    public void validateTokenOrThrow(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            Date expiration = claims.getBody().getExpiration();
            if (expiration.before(new Date())) {
                log.warn("❌ JWT 만료됨: {}", expiration);
                throw new JwtException("토큰이 만료되었습니다."); // ✅ 401 유도
            }

            log.info("✅ JWT 유효성 검사 통과");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("❌ JWT 유효성 검사 실패: {}", e.getMessage());
            throw new JwtException("유효하지 않은 토큰입니다."); // ✅ 401 유도
        }
    }


    public Long getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long userId = Long.parseLong(claims.getSubject());
        log.info("🔍 JWT에서 추출한 userId: {}", userId);
        return userId;
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.info("📦 Authorization 헤더에서 토큰 추출 완료");
            return token;
        }
        log.warn("❌ Authorization 헤더 없음 또는 형식 오류");
        return null;
    }

    public Authentication getAuthentication(String token) {
        Long userId = getUserId(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ 사용자 정보 조회 실패: userId={}", userId);
                    return new RuntimeException("User not found");
                });

        CustomUserPrincipal principal = new CustomUserPrincipal(user, Map.of());
        log.info("✅ 인증 객체 생성 완료: userId={}, role={}", user.getUserId(), user.getRole());

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    public String createToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationTime);

        String token = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.info("✅ 이메일 기반 JWT 생성 완료: email={}, 만료={}", email, expiry);
        return token;
    }
}