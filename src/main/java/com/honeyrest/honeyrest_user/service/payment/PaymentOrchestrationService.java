package com.honeyrest.honeyrest_user.service.payment;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.entity.Coupon;
import com.honeyrest.honeyrest_user.entity.Payment;
import com.honeyrest.honeyrest_user.entity.Reservation;
import com.honeyrest.honeyrest_user.entity.Room;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.entity.UserCoupon;
import com.honeyrest.honeyrest_user.mapper.ReservationMapper;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.coupon.UserCouponRepository;
import com.honeyrest.honeyrest_user.repository.payment.PaymentRepository;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import com.honeyrest.honeyrest_user.repository.room.RoomRepository;
import com.honeyrest.honeyrest_user.service.email.EmailService;
import com.honeyrest.honeyrest_user.service.reservation.ReserveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private final TossService tossService;
    private final ReserveService reserveService;
    private final PaymentService paymentService;
    private final PaymentDetailService paymentDetailService;
    private final ReservationMapper reservationMapper;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    public ReservationCompleteDTO confirmAndSave(TossConfirmRequest request) throws Exception {
        TossPaymentResult result = tossService.confirmPayment(request);

        if ("FAILED".equals(result.getStatus())) {
            throw new IllegalStateException("결제 승인 실패");
        }

        validatePaymentConsistency(request, result);

        ReservationCompleteDTO dto = transactionTemplate.execute(status -> {
            if (paymentRepository.existsByTransactionId(result.getPaymentKey())) {
                throw new IllegalStateException("이미 처리된 결제입니다.");
            }

            ReservationRequestDTO reservationInfo = request.getReservationInfo();
            if (reservationRepository.existsByReservationNumber(reservationInfo.getReservationCode())) {
                throw new IllegalStateException("이미 처리된 예약번호입니다.");
            }

            Reservation reservation = reserveService.createReservation(
                    reservationInfo,
                    result.getAmount(),
                    reservationInfo.getDiscountAmount()
            );

            Payment payment = paymentService.saveTossPayment(result, reservation);
            paymentDetailService.saveTossDetails(result, payment);

            ReservationCompleteDTO created = reservationMapper.toCompleteDTO(reservation, payment);
            created.setCouponName(result.getCouponName());
            created.setIsEmailSent(false);
            return created;
        });

        if (Boolean.TRUE.equals(result.getIsEmailSend()) && dto != null && dto.getGuestEmail() != null) {
            emailService.sendReservationConfirmation(dto.getGuestEmail(), dto);
            dto.setIsEmailSent(true);
        }

        return dto;
    }

    private void validatePaymentConsistency(TossConfirmRequest request, TossPaymentResult result) {
        if (request.getReservationInfo() == null) {
            throw new IllegalArgumentException("예약 정보가 없습니다.");
        }

        ReservationRequestDTO reservationInfo = request.getReservationInfo();
        if (reservationInfo.getReservationCode() == null || reservationInfo.getReservationCode().isBlank()) {
            throw new IllegalArgumentException("예약번호가 없습니다.");
        }

        if (!reservationInfo.getReservationCode().equals(result.getOrderId())) {
            throw new IllegalArgumentException("주문번호 불일치");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(result.getAmount()) != 0) {
            throw new IllegalArgumentException("결제 금액 불일치");
        }

        NormalizedAmount normalized = normalizeAndValidateAmount(reservationInfo);

        if (normalized.finalAmount().compareTo(result.getAmount()) != 0) {
            throw new IllegalArgumentException("서버 계산 금액과 결제 금액이 다릅니다.");
        }

        reservationInfo.setOriginalPrice(normalized.originalPrice());
        reservationInfo.setDiscountAmount(normalized.discountAmount());
        reservationInfo.setUsedPoint(normalized.usedPoint());
    }

    private NormalizedAmount normalizeAndValidateAmount(ReservationRequestDTO request) {
        if (request.getRoomId() == null || request.getCheckIn() == null || request.getCheckOut() == null || request.getGuests() == null) {
            throw new IllegalArgumentException("예약 필수값이 누락되었습니다.");
        }

        if (!request.getCheckIn().isBefore(request.getCheckOut())) {
            throw new IllegalArgumentException("체크인/체크아웃 날짜가 올바르지 않습니다.");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 객실입니다."));

        if (request.getGuests() <= 0 || request.getGuests() > room.getMaxOccupancy()) {
            throw new IllegalArgumentException("예약 인원이 허용 범위를 벗어났습니다.");
        }

        BigDecimal original = calculateOriginalPrice(room, request.getCheckIn(), request.getCheckOut(), request.getGuests());
        BigDecimal couponDiscount = calculateCouponDiscount(original, request);
        Integer usedPoint = normalizeUsedPoint(request);

        BigDecimal finalAmount = original.subtract(couponDiscount).subtract(BigDecimal.valueOf(usedPoint));
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("최종 결제 금액이 올바르지 않습니다.");
        }

        return new NormalizedAmount(
                original.setScale(2, RoundingMode.HALF_UP),
                couponDiscount.setScale(2, RoundingMode.HALF_UP),
                usedPoint,
                finalAmount.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal calculateOriginalPrice(Room room, LocalDate checkIn, LocalDate checkOut, int guests) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal basePrice = room.getPrice().multiply(BigDecimal.valueOf(nights));

        int extraCount = Math.max(0, guests - room.getStandardOccupancy());
        BigDecimal extraFeePerNight = room.getExtraPersonFee() == null ? BigDecimal.ZERO : room.getExtraPersonFee();
        BigDecimal extraFee = extraFeePerNight.multiply(BigDecimal.valueOf(extraCount)).multiply(BigDecimal.valueOf(nights));

        return basePrice.add(extraFee);
    }

    private BigDecimal calculateCouponDiscount(BigDecimal original, ReservationRequestDTO request) {
        if (request.getCouponId() == null) {
            return BigDecimal.ZERO;
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("회원 쿠폰은 로그인 사용자만 사용할 수 있습니다.");
        }

        UserCoupon userCoupon = userCouponRepository.findUserCouponByUserCouponId(request.getCouponId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 쿠폰을 찾을 수 없습니다."));

        if (!userCoupon.getUser().getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("쿠폰 사용자 정보가 일치하지 않습니다.");
        }

        if (!"ISSUED".equals(userCoupon.getStatus())) {
            throw new IllegalArgumentException("사용 가능한 쿠폰이 아닙니다.");
        }

        if (userCoupon.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료된 쿠폰입니다.");
        }

        Coupon coupon = userCoupon.getCoupon();
        if (!coupon.isActive()) {
            throw new IllegalArgumentException("비활성 쿠폰입니다.");
        }

        if (coupon.getStartDate().isAfter(LocalDateTime.now()) || coupon.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("사용 기간이 아닌 쿠폰입니다.");
        }

        if (coupon.getMinOrderAmount() != null && original.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("쿠폰 최소 주문 금액 조건을 충족하지 않습니다.");
        }

        BigDecimal discount;
        if ("RATE".equalsIgnoreCase(coupon.getDiscountType()) || "PERCENT".equalsIgnoreCase(coupon.getDiscountType())) {
            discount = original.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue();
        }

        if (coupon.getMaxOrderAmount() != null && discount.compareTo(coupon.getMaxOrderAmount()) > 0) {
            discount = coupon.getMaxOrderAmount();
        }

        if (discount.compareTo(original) > 0) {
            discount = original;
        }

        return discount.max(BigDecimal.ZERO);
    }

    private Integer normalizeUsedPoint(ReservationRequestDTO request) {
        Integer usedPoint = request.getUsedPoint() == null ? 0 : request.getUsedPoint();
        if (usedPoint < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }

        if (usedPoint == 0) {
            return 0;
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("포인트는 로그인 사용자만 사용할 수 있습니다.");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        if (user.getPoint() < usedPoint) {
            throw new IllegalArgumentException("사용 가능한 포인트가 부족합니다.");
        }

        return usedPoint;
    }

    private record NormalizedAmount(
            BigDecimal originalPrice,
            BigDecimal discountAmount,
            Integer usedPoint,
            BigDecimal finalAmount
    ) {
    }
}
