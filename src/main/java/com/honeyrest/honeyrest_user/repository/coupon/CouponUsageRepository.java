package com.honeyrest.honeyrest_user.repository.coupon;

import com.honeyrest.domain.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    /** 이 사용자 쿠폰으로 이미 사용 기록(coupon_usage)이 있는지. 목록 조회의 notExists 조건과 같은 기준. */
    boolean existsByUserCoupon_UserCouponId(Long userCouponId);

}
