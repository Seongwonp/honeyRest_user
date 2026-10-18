package com.honeyrest.honeyrest_user.service.coupon;


import com.honeyrest.honeyrest_user.dto.coupon.AvailableCouponDTO;
import com.honeyrest.domain.entity.Coupon;
import com.honeyrest.domain.entity.UserCoupon;
import com.honeyrest.honeyrest_user.repository.coupon.CouponUsageRepository;
import com.honeyrest.honeyrest_user.repository.coupon.UserCouponQueryRepository;
import com.honeyrest.honeyrest_user.repository.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class CouponService {

    private final UserCouponQueryRepository userCouponQueryRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponUsageRepository couponUsageRepository;

    public List<AvailableCouponDTO> getAvailableCoupons(Long userId, BigDecimal originalPrice, Long accommodationId) {
        List<UserCoupon> coupons = userCouponQueryRepository.findAvailableCoupons(userId, originalPrice, accommodationId);

        return coupons.stream()
                .map(userCoupon -> {
                    Coupon coupon = userCoupon.getCoupon();
                    return AvailableCouponDTO.builder()
                            // 결제 요청에는 userCouponId 를 보낸다 (결제는 user_coupon id 로 조회한다).
                            .userCouponId(userCoupon.getUserCouponId())
                            .couponId(coupon.getCouponId())
                            .name(coupon.getName())
                            .discountType(coupon.getDiscountType())
                            .discountValue(coupon.getDiscountValue())
                            .minOrderAmount(coupon.getMinOrderAmount())
                            .maxOrderAmount(coupon.getMaxDiscountAmount())
                            .build();
                })
                .toList();
    }

    /**
     * 결제 시 선택한 사용자 쿠폰을 검증하고 할인액(원 단위)을 계산한다.
     * 사용 가능 쿠폰 목록({@link UserCouponQueryRepository#findAvailableCoupons})과 같은 조건을 결제 시점에 다시 확인한다.
     * 과거에는 목록에서만 적용 대상·사용 이력을 걸러 결제 요청을 직접 만들면 다른 숙소 전용 쿠폰이나
     * 이미 사용 기록이 있는 쿠폰도 적용됐다.
     * <ul>
     *   <li>본인 소유, 상태 ISSUED, 사용자 쿠폰 만료 전</li>
     *   <li>쿠폰 활성 + 사용 기간 안</li>
     *   <li>최소 주문 금액 (NULL 이면 제한 없음)</li>
     *   <li>적용 대상: targetType ALL/ACCOMMODATION, targetId 가 있으면 해당 숙소만</li>
     *   <li>coupon_usage 에 사용 기록이 없을 것</li>
     * </ul>
     * 실제 사용 처리(이중 사용 방지)는 예약 저장 트랜잭션의 {@link CouponUsageService#recordUsage} 가 원자적으로 한다.
     *
     * @throws IllegalArgumentException 검증 실패 (400, 결제 승인 전이라 과금 없음)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscountForPayment(Long userId, Long userCouponId,
                                                  BigDecimal originalPrice, Long accommodationId) {
        if (userCouponId == null) {
            return BigDecimal.ZERO;
        }
        if (userId == null) {
            throw new IllegalArgumentException("회원 쿠폰은 로그인 사용자만 사용할 수 있습니다.");
        }

        UserCoupon userCoupon = userCouponRepository.findUserCouponByUserCouponId(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰을 찾을 수 없습니다."));

        if (userCoupon.getUser() == null || !userId.equals(userCoupon.getUser().getUserId())) {
            throw new IllegalArgumentException("쿠폰 사용자 정보가 일치하지 않습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!"ISSUED".equals(userCoupon.getStatus())) {
            throw new IllegalArgumentException("사용 가능한 쿠폰이 아닙니다.");
        }
        if (!userCoupon.isIssuedAndUnexpired(now)) {
            throw new IllegalArgumentException("만료된 쿠폰입니다.");
        }

        Coupon coupon = userCoupon.getCoupon();
        if (!coupon.isActive()) {
            throw new IllegalArgumentException("비활성 쿠폰입니다.");
        }
        if (!coupon.isUsableAt(now)) {
            throw new IllegalArgumentException("사용 기간이 아닌 쿠폰입니다.");
        }
        if (!coupon.meetsMinOrder(originalPrice)) {
            throw new IllegalArgumentException("쿠폰 최소 주문 금액 조건을 충족하지 않습니다.");
        }
        if (!coupon.isApplicableToAccommodation(accommodationId)) {
            throw new IllegalArgumentException("이 숙소에는 사용할 수 없는 쿠폰입니다.");
        }
        if (couponUsageRepository.existsByUserCoupon_UserCouponId(userCouponId)) {
            throw new IllegalArgumentException("이미 사용된 쿠폰입니다.");
        }

        // 할인액 계산(정률/정액, 최대 할인·주문 금액 상한, 원 단위 버림)은 쿠폰 도메인이 담당한다.
        return coupon.calculateDiscount(originalPrice);
    }
}
