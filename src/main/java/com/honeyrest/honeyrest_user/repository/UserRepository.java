package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findBySocialTypeAndSocialId(String socialType, String socialId);

    /**
     * 포인트 원자적 차감: 잔액이 충분할 때만 차감한다 (UPDATE ... WHERE point >= :amount).
     *
     * @return 변경된 행 수 (0 이면 잔액 부족 또는 사용자 없음)
     */
    @Modifying(flushAutomatically = true)
    @Query("update User u set u.point = u.point - :amount where u.userId = :userId and u.point >= :amount")
    int deductPointIfEnough(@Param("userId") Long userId, @Param("amount") int amount);

    /** 포인트 원자적 증가 (적립·환불). @return 변경된 행 수 (0 이면 사용자 없음) */
    @Modifying(flushAutomatically = true)
    @Query("update User u set u.point = u.point + :amount where u.userId = :userId")
    int addPoint(@Param("userId") Long userId, @Param("amount") int amount);

    /** 현재 DB 잔액 (영속성 컨텍스트의 엔티티 값이 아니라 같은 트랜잭션에서 방금 UPDATE 한 결과를 읽는다). */
    @Query("select u.point from User u where u.userId = :userId")
    Optional<Integer> findPointByUserId(@Param("userId") Long userId);
}
