package com.honeyrest.honeyrest_user.repository.coupon;

import com.honeyrest.domain.entity.QCoupon;
import com.honeyrest.domain.entity.QCouponUsage;
import com.honeyrest.domain.entity.QUserCoupon;
import com.honeyrest.domain.entity.UserCoupon;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserCouponQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 예약 폼의 사용 가능 쿠폰. 결제 시점 검증(CouponService#calculateDiscountForPayment)과 같은 조건을 쓴다.
     * 최소 주문 금액이 NULL 이면 제한 없음으로 본다.
     */
    public List<UserCoupon> findAvailableCoupons(Long userId, BigDecimal originalPrice, Long accommodationId) {
        LocalDateTime now = LocalDateTime.now();
        QUserCoupon userCoupon = QUserCoupon.userCoupon;
        QCoupon coupon = QCoupon.coupon;
        QCouponUsage usage = QCouponUsage.couponUsage;

        return queryFactory
                .selectFrom(userCoupon)
                .join(userCoupon.coupon, coupon).fetchJoin()
                .where(
                        userCoupon.user.userId.eq(userId),
                        userCoupon.status.eq("ISSUED"),
                        coupon.isActive.isTrue(),
                        userCoupon.expiredAt.goe(now),
                        coupon.startDate.loe(now),
                        coupon.endDate.goe(now),
                        coupon.minOrderAmount.isNull().or(coupon.minOrderAmount.loe(originalPrice)),
                        coupon.targetType.in("ALL", "ACCOMMODATION"),
                        coupon.targetId.isNull().or(coupon.targetId.eq(accommodationId)),
                        com.querydsl.jpa.JPAExpressions.selectOne()
                                .from(usage)
                                .where(usage.userCoupon.eq(userCoupon))
                                .notExists()
                )
                .fetch();
    }
}