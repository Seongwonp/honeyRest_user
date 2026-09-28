package com.honeyrest.honeyrest_user.repository.coupon;

import com.honeyrest.domain.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
