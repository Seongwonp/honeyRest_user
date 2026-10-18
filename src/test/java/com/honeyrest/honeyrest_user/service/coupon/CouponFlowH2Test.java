package com.honeyrest.honeyrest_user.service.coupon;

import com.honeyrest.domain.entity.*;
import com.honeyrest.domain.type.ReservationStatus;
import com.honeyrest.honeyrest_user.dto.coupon.AvailableCouponDTO;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.repository.coupon.CouponUsageRepository;
import com.honeyrest.honeyrest_user.support.ConcurrencyResult;
import com.honeyrest.honeyrest_user.support.H2IntegrationTestSupport;
import com.honeyrest.honeyrest_user.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 쿠폰 id 계약(user_coupon id), 결제 시점 검증, 원자적 사용 처리를 실제 JPA 쿼리(H2)로 확인한다.
 */
@DisplayName("쿠폰: user_coupon id 계약 · 결제 검증 · 원자적 사용 (H2)")
class CouponFlowH2Test extends H2IntegrationTestSupport {

    private static final BigDecimal PRICE = new BigDecimal("100000.00");

    @Autowired private CouponService couponService;
    @Autowired private CouponUsageService couponUsageService;
    @Autowired private CouponUsageRepository couponUsageRepository;

    private User user;
    private Accommodation accommodation;
    private Room room;

    @BeforeEach
    void setUp() {
        user = fixtures.user(0);
        accommodation = fixtures.accommodation(TestFixtures.unique("쿠폰시"), "ACTIVE");
        room = fixtures.room(accommodation.getAccommodationId(), 5, 4, "ACTIVE");
    }

    /** user_coupon id 가 coupon id 와 같아지지 않도록 다른 사용자의 발급 행을 몇 개 먼저 만든다. */
    private UserCoupon issueWithDistinctIds(Coupon coupon) {
        User other = fixtures.user(0);
        UserCoupon issued;
        int guard = 0;
        do {
            fixtures.userCoupon(other.getUserId(), coupon.getCouponId(), "ISSUED");
            issued = fixtures.userCoupon(user.getUserId(), coupon.getCouponId(), "ISSUED");
        } while (issued.getUserCouponId().equals(coupon.getCouponId()) && ++guard < 5);
        assertThat(issued.getUserCouponId()).isNotEqualTo(coupon.getCouponId());
        return issued;
    }

    @Test
    @DisplayName("사용 가능 쿠폰 목록은 결제에 보낼 userCouponId 와 표시용 couponId 를 구분해 내려준다")
    void availableCouponsExposeUserCouponId() {
        Coupon coupon = fixtures.coupon(b -> {});
        UserCoupon issued = issueWithDistinctIds(coupon);

        List<AvailableCouponDTO> list = couponService.getAvailableCoupons(user.getUserId(), PRICE, accommodation.getAccommodationId());

        assertThat(list).singleElement().satisfies(dto -> {
            assertThat(dto.getUserCouponId()).isEqualTo(issued.getUserCouponId());
            assertThat(dto.getCouponId()).isEqualTo(coupon.getCouponId());
        });
        // 결제는 목록의 userCouponId 로 그대로 검증·계산된다.
        assertThat(couponService.calculateDiscountForPayment(user.getUserId(), list.get(0).getUserCouponId(),
                PRICE, accommodation.getAccommodationId())).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("최소 주문 금액이 NULL 인 쿠폰도 목록에 나온다 (NULL = 최소 금액 없음)")
    void nullMinOrderCouponIsListed() {
        Coupon noMin = fixtures.coupon(b -> b.minOrderAmount(null));
        Coupon tooHigh = fixtures.coupon(b -> b.minOrderAmount(new BigDecimal("200000")));
        fixtures.userCoupon(user.getUserId(), noMin.getCouponId(), "ISSUED");
        fixtures.userCoupon(user.getUserId(), tooHigh.getCouponId(), "ISSUED");

        List<AvailableCouponDTO> list = couponService.getAvailableCoupons(user.getUserId(), PRICE, accommodation.getAccommodationId());

        assertThat(list).extracting(AvailableCouponDTO::getCouponId).containsExactly(noMin.getCouponId());
    }

    @Test
    @DisplayName("결제 검증: 다른 숙소 전용 쿠폰은 거절한다 (목록과 같은 적용 대상 규칙)")
    void paymentRejectsOtherAccommodationCoupon() {
        Coupon otherOnly = fixtures.coupon(b -> b.targetType("ACCOMMODATION").targetId(accommodation.getAccommodationId() + 999));
        UserCoupon issued = fixtures.userCoupon(user.getUserId(), otherOnly.getCouponId(), "ISSUED");

        assertThatThrownBy(() -> couponService.calculateDiscountForPayment(
                user.getUserId(), issued.getUserCouponId(), PRICE, accommodation.getAccommodationId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용할 수 없는 쿠폰");
    }

    @Test
    @DisplayName("결제 검증: coupon_usage 에 사용 기록이 있으면 상태가 ISSUED 여도 거절한다")
    void paymentRejectsCouponWithUsageRecord() {
        Coupon coupon = fixtures.coupon(b -> {});
        UserCoupon issued = fixtures.userCoupon(user.getUserId(), coupon.getCouponId(), "ISSUED");
        Reservation reservation = fixtures.reservation(user.getUserId(), room.getRoomId(), ReservationStatus.CONFIRMED,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        transactionTemplate.executeWithoutResult(s -> entityManager.persist(CouponUsage.builder()
                .userCoupon(entityManager.getReference(UserCoupon.class, issued.getUserCouponId()))
                .reservation(entityManager.getReference(Reservation.class, reservation.getReservationId()))
                .discountAmount(new BigDecimal("5000"))
                .build()));

        assertThatThrownBy(() -> couponService.calculateDiscountForPayment(
                user.getUserId(), issued.getUserCouponId(), PRICE, accommodation.getAccommodationId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용된 쿠폰");
    }

    @Test
    @DisplayName("결제 검증: 최소 주문 금액 NULL 은 통과, 미달은 거절, 타인 쿠폰은 거절")
    void paymentMinOrderAndOwnership() {
        Coupon noMin = fixtures.coupon(b -> b.minOrderAmount(null));
        Coupon min = fixtures.coupon(b -> b.minOrderAmount(new BigDecimal("200000")));
        UserCoupon noMinIssued = fixtures.userCoupon(user.getUserId(), noMin.getCouponId(), "ISSUED");
        UserCoupon minIssued = fixtures.userCoupon(user.getUserId(), min.getCouponId(), "ISSUED");
        Long accId = accommodation.getAccommodationId();

        assertThat(couponService.calculateDiscountForPayment(user.getUserId(), noMinIssued.getUserCouponId(), PRICE, accId))
                .isEqualByComparingTo("5000");
        assertThatThrownBy(() -> couponService.calculateDiscountForPayment(user.getUserId(), minIssued.getUserCouponId(), PRICE, accId))
                .hasMessageContaining("최소 주문 금액");
        User stranger = fixtures.user(0);
        assertThatThrownBy(() -> couponService.calculateDiscountForPayment(stranger.getUserId(), noMinIssued.getUserCouponId(), PRICE, accId))
                .hasMessageContaining("쿠폰 사용자 정보");
    }

    @Test
    @DisplayName("사용 처리: 같은 쿠폰을 두 번 사용 처리하면 두 번째는 409 (조건부 UPDATE)")
    void secondUsageIsConflict() {
        Coupon coupon = fixtures.coupon(b -> {});
        UserCoupon issued = fixtures.userCoupon(user.getUserId(), coupon.getCouponId(), "ISSUED");
        Reservation first = fixtures.reservation(user.getUserId(), room.getRoomId(), ReservationStatus.CONFIRMED,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        Reservation second = fixtures.reservation(user.getUserId(), room.getRoomId(), ReservationStatus.CONFIRMED,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(4));

        couponUsageService.recordUsage(issued.getUserCouponId(), user.getUserId(), first, new BigDecimal("5000"));

        assertThatThrownBy(() -> couponUsageService.recordUsage(issued.getUserCouponId(), user.getUserId(), second, new BigDecimal("5000")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(fixtures.find(UserCoupon.class, issued.getUserCouponId()).getStatus()).isEqualTo("USED");
        assertThat(couponUsageRepository.existsByUserCoupon_UserCouponId(issued.getUserCouponId())).isTrue();
    }

    @Test
    @DisplayName("사용 처리: 다른 사용자의 쿠폰은 사용 처리할 수 없다 (409, 상태 그대로)")
    void cannotUseOthersCoupon() {
        Coupon coupon = fixtures.coupon(b -> {});
        UserCoupon issued = fixtures.userCoupon(user.getUserId(), coupon.getCouponId(), "ISSUED");
        User stranger = fixtures.user(0);
        Reservation reservation = fixtures.reservation(stranger.getUserId(), room.getRoomId(), ReservationStatus.CONFIRMED,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        assertThatThrownBy(() -> couponUsageService.recordUsage(issued.getUserCouponId(), stranger.getUserId(), reservation, new BigDecimal("5000")))
                .isInstanceOf(ApiException.class);
        assertThat(fixtures.find(UserCoupon.class, issued.getUserCouponId()).getStatus()).isEqualTo("ISSUED");
    }

    @Test
    @DisplayName("사용 처리: 동시에 같은 쿠폰을 사용하면 정확히 하나만 성공한다")
    void concurrentUsageOnlyOneSucceeds() throws Exception {
        Coupon coupon = fixtures.coupon(b -> {});
        UserCoupon issued = fixtures.userCoupon(user.getUserId(), coupon.getCouponId(), "ISSUED");
        int threads = 4;
        List<Reservation> reservations = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            reservations.add(fixtures.reservation(user.getUserId(), room.getRoomId(), ReservationStatus.CONFIRMED,
                    LocalDate.now().plusDays(10 + i * 2L), LocalDate.now().plusDays(11 + i * 2L)));
        }

        ConcurrencyResult result = ConcurrencyResult.run(threads, i ->
                couponUsageService.recordUsage(issued.getUserCouponId(), user.getUserId(), reservations.get(i), new BigDecimal("5000")));

        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.failures()).hasSize(threads - 1)
                .allSatisfy(e -> assertThat(e).isInstanceOf(ApiException.class));
    }
}
