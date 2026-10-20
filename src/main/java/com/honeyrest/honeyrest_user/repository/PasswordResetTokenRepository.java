package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.domain.entity.PasswordResetToken;
import com.honeyrest.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenValue(String tokenValue);

    /**
     * 토큰을 원자적으로 소비(삭제)한다. 0 이면 이미 다른 요청이 사용한 토큰이다 (동시 요청 중 하나만 1).
     */
    @Modifying
    @Query("delete from PasswordResetToken t where t.tokenValue = :tokenValue")
    int consumeByTokenValue(@Param("tokenValue") String tokenValue);

    /** 새 재설정 토큰을 발급할 때 이전 토큰을 모두 무효화한다 (가장 최근 메일의 링크만 유효). */
    @Modifying
    @Query("delete from PasswordResetToken t where t.user = :user")
    int deleteAllByUser(@Param("user") User user);
}
