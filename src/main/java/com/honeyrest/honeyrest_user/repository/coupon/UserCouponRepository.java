package com.honeyrest.honeyrest_user.repository.coupon;

import com.honeyrest.honeyrest_user.entity.Coupon;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    Optional<UserCoupon> findByUserAndCoupon(User user, Coupon coupon);

    Optional<UserCoupon> findUserCouponByUserCouponId(Long userCouponId);
}
