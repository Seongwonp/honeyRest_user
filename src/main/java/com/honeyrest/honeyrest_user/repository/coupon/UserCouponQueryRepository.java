package com.honeyrest.honeyrest_user.repository.coupon;

import com.honeyrest.honeyrest_user.entity.QCoupon;
import com.honeyrest.honeyrest_user.entity.QUserCoupon;
import com.honeyrest.honeyrest_user.entity.UserCoupon;
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

    public List<UserCoupon> findAvailableCoupons(Long userId, BigDecimal originalPrice, Long accommodationId) {
        QUserCoupon userCoupon = QUserCoupon.userCoupon;
        QCoupon coupon = QCoupon.coupon;

        return queryFactory
                .selectFrom(userCoupon)
                .join(userCoupon.coupon, coupon).fetchJoin()
                .where(
                        userCoupon.user.userId.eq(userId),
                        userCoupon.status.eq("ISSUED"),
                        coupon.isActive.isTrue(),
                        coupon.startDate.loe(LocalDateTime.now()),
                        coupon.endDate.goe(LocalDateTime.now()),
                        coupon.minOrderAmount.loe(originalPrice),
                        coupon.targetType.in("ALL", "ACCOMMODATION"),
                        coupon.targetId.isNull().or(coupon.targetId.eq(accommodationId))
                )
                .fetch();
    }
}