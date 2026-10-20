package com.honeyrest.honeyrest_user.service.coupon;


import com.honeyrest.domain.entity.CouponUsage;
import com.honeyrest.domain.entity.Reservation;
import com.honeyrest.domain.entity.UserCoupon;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.repository.coupon.CouponUsageRepository;
import com.honeyrest.honeyrest_user.repository.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Log4j2
@Service
@RequiredArgsConstructor
public class CouponUsageService {
    private final CouponUsageRepository couponUsageRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 쿠폰 사용 처리 + 사용 기록(coupon_usage) 저장.
     * 조건부 UPDATE(본인 소유 + status='ISSUED')로 동시 결제 중 하나만 성공하고, 나머지는 409 로 롤백된다.
     *
     * @param userCouponId 사용자 쿠폰(user_coupon) id
     * @param userId       결제한 로그인 사용자 id (소유권 조건)
     */
    @Transactional
    public void recordUsage(Long userCouponId, Long userId, Reservation reservation, BigDecimal discountAmount) {
        if (userId == null) {
            throw new IllegalArgumentException("회원 쿠폰은 로그인 사용자만 사용할 수 있습니다.");
        }

        int updated = userCouponRepository.markUsedIfIssued(userCouponId, userId, LocalDateTime.now());
        if (updated == 0) {
            throw new ApiException("이미 사용되었거나 사용할 수 없는 쿠폰입니다.", HttpStatus.CONFLICT);
        }

        UserCoupon coupon = userCouponRepository.getReferenceById(userCouponId);
        CouponUsage usage = CouponUsage.builder()
                .userCoupon(coupon)
                .reservation(reservation)
                .discountAmount(discountAmount)
                .build();

        couponUsageRepository.save(usage);

        log.info("🎟️ 쿠폰 사용 처리 완료: userCouponId={}, userId={}, reservationId={}, discountAmount={}",
                userCouponId, userId, reservation.getReservationId(), discountAmount);
    }
}
