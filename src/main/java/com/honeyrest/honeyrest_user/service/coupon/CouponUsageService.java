package com.honeyrest.honeyrest_user.service.coupon;


import com.honeyrest.honeyrest_user.entity.CouponUsage;
import com.honeyrest.honeyrest_user.entity.Reservation;
import com.honeyrest.honeyrest_user.entity.UserCoupon;
import com.honeyrest.honeyrest_user.repository.coupon.CouponUsageRepository;
import com.honeyrest.honeyrest_user.repository.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Log4j2
@Service
@RequiredArgsConstructor
public class CouponUsageService {
    private final CouponUsageRepository couponUsageRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public void recordUsage(Long userCouponId, Reservation reservation, BigDecimal discountAmount) {
        UserCoupon coupon = userCouponRepository.findUserCouponByUserCouponId(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰을 찾을 수 없습니다"));

        coupon.use();
        userCouponRepository.save(coupon);

        CouponUsage usage = CouponUsage.builder()
                .userCoupon(coupon)
                .reservation(reservation)
                .discountAmount(discountAmount)
                .build();

        couponUsageRepository.save(usage);

        log.info("🎟️ 쿠폰 사용 처리 완료: couponId={}, userId={}, reservation={}, discountAmount={}",
                coupon.getCoupon().getCouponId(),
                coupon.getUser().getUserId(),
                reservation,
                discountAmount);


    }



}
