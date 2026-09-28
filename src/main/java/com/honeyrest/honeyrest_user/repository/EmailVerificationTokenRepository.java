package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.domain.entity.EmailVerificationToken;
import com.honeyrest.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenValue(String tokenValue);

    List<EmailVerificationToken> findByUser(User user);

    void deleteByUser(User user);

    /**
     * 토큰을 원자적으로 소비(삭제)한다. 0 이면 이미 사용된 토큰이다 (1회용 보장).
     */
    @Modifying
    @Query("delete from EmailVerificationToken t where t.tokenValue = :tokenValue")
    int consumeByTokenValue(@Param("tokenValue") String tokenValue);

    /** 같은 용도의 이전 토큰을 무효화한다 (가장 최근 메일의 링크만 유효). */
    @Modifying
    @Query("delete from EmailVerificationToken t where t.user = :user and t.tokenType = :tokenType")
    int deleteAllByUserAndTokenType(@Param("user") User user, @Param("tokenType") String tokenType);

}
