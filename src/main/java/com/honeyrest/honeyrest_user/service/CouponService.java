package com.honeyrest.honeyrest_user.service;


import com.honeyrest.honeyrest_user.dto.coupon.AvailableCouponDTO;
import com.honeyrest.honeyrest_user.entity.Coupon;
import com.honeyrest.honeyrest_user.entity.UserCoupon;
import com.honeyrest.honeyrest_user.repository.coupon.UserCouponQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class CouponService {

    private final UserCouponQueryRepository userCouponQueryRepository;

    public List<AvailableCouponDTO> getAvailableCoupons(Long userId, BigDecimal originalPrice, Long accommodationId) {
        List<UserCoupon> coupons = userCouponQueryRepository.findAvailableCoupons(userId, originalPrice, accommodationId);

        return coupons.stream()
                .map(userCupon -> {
                    Coupon coupon = userCupon.getCoupon();
                    return AvailableCouponDTO.builder()
                            .couponId(coupon.getCouponId())
                            .name(coupon.getName())
                            .discountType(coupon.getDiscountType())
                            .discountValue(coupon.getDiscountValue())
                            .minOrderAmount(coupon.getMinOrderAmount())
                            .maxOrderAmount(coupon.getMaxOrderAmount())
                            .build();
                })
                .toList();
    }
}