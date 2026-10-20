package com.honeyrest.honeyrest_user.repository.coupon;

import com.honeyrest.domain.entity.Coupon;
import com.honeyrest.domain.entity.User;
import com.honeyrest.domain.entity.UserCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    Optional<UserCoupon> findUserCouponByUserCouponId(Long userCouponId);

    /**
     * 본인 소유이고 아직 ISSUED 인 쿠폰만 USED 로 바꾼다. 조건부 UPDATE 라 동시 결제 중 하나만 1 을 받는다.
     *
     * @return 변경된 행 수 (0 이면 이미 사용됐거나, 본인 쿠폰이 아니거나, 존재하지 않음 → 호출자가 409 처리)
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update UserCoupon uc
               set uc.status = 'USED', uc.usedAt = :usedAt
             where uc.userCouponId = :userCouponId
               and uc.user.userId = :userId
               and uc.status = 'ISSUED'
            """)
    int markUsedIfIssued(@Param("userCouponId") Long userCouponId,
                         @Param("userId") Long userId,
                         @Param("usedAt") LocalDateTime usedAt);

    Page<UserCoupon> findAllByUserUserId(Long userId, Pageable pageable);
    Page<UserCoupon> findAllByUserUserIdAndCouponDiscountType(Long userId, String discountType, Pageable pageable);
}
