package com.honeyrest.honeyrest_user.service.payment;

import com.honeyrest.honeyrest_user.dto.payment.toss.TossConfirmRequest;
import com.honeyrest.honeyrest_user.dto.payment.toss.TossPaymentResult;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.domain.entity.Payment;
import com.honeyrest.domain.entity.Reservation;
import com.honeyrest.domain.entity.Room;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.mapper.ReservationMapper;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.payment.PaymentRepository;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import com.honeyrest.honeyrest_user.repository.room.RoomRepository;
import com.honeyrest.honeyrest_user.service.coupon.CouponService;
import com.honeyrest.honeyrest_user.service.email.EmailService;
import com.honeyrest.honeyrest_user.service.reservation.PriceCalculator;
import com.honeyrest.honeyrest_user.service.reservation.ReserveService;
import com.honeyrest.honeyrest_user.service.reservation.StayDatePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    private final CouponService couponService;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PriceCalculator priceCalculator;

    /**
     * 결제 승인 + 예약 저장.
     * <ol>
     *   <li>사전 검증 (토스 호출 전, 과금 없음): 중복 결제/예약번호, 주문번호·금액 일치, 서버 금액 재계산, 재고(락 없음)</li>
     *   <li>토스 승인 (실제 과금)</li>
     *   <li>트랜잭션 안에서 객실 행 락(트랜잭션의 첫 문장) + 잠금 읽기로 재고 재확인 후 예약/결제 저장</li>
     *   <li>3 단계가 실패하면 토스 결제를 취소(보상)하고 사용자에게 결제가 취소되었음을 알린다</li>
     * </ol>
     */
    public ReservationCompleteDTO confirmAndSave(TossConfirmRequest request) {
        ReservationRequestDTO reservationInfo = request.getReservationInfo();

        // 1. 사전 검증 — 여기서 실패하면 아직 과금 전이므로 보상이 필요 없다.
        if (paymentRepository.existsByTransactionId(request.getPaymentKey())) {
            throw new ApiException("이미 처리된 결제입니다.", HttpStatus.CONFLICT);
        }
        validateRequest(request);
        if (reservationRepository.existsByReservationNumber(reservationInfo.getReservationCode())) {
            throw new ApiException("이미 처리된 예약번호입니다.", HttpStatus.CONFLICT);
        }
        Room room = roomRepository.findById(reservationInfo.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 객실입니다."));
        NormalizedAmount normalized = normalizeAndValidateAmount(room, reservationInfo);
        if (normalized.finalAmount().compareTo(request.getAmount()) != 0) {
            throw new IllegalArgumentException("서버 계산 금액과 결제 금액이 다릅니다.");
        }
        reserveService.assertRoomAvailable(room, reservationInfo.getCheckIn(), reservationInfo.getCheckOut());

        reservationInfo.setOriginalPrice(normalized.originalPrice());
        reservationInfo.setDiscountAmount(normalized.discountAmount());
        reservationInfo.setUsedPoint(normalized.usedPoint());

        // 2. 토스 승인 — 2xx 가 아니면 TossService 가 ApiException 을 던진다 (과금 없음).
        //    타임아웃이면 TossService 가 주문번호로 결제를 조회해 승인된 결제를 취소한 뒤
        //    PaymentConfirmTimeoutException(504) 을 던진다. 아직 예약을 만들기 전이므로 여기서 더 할 일은 없다.
        TossPaymentResult result = tossService.confirmPayment(request);
        if ("FAILED".equals(result.getStatus())) {
            throw new ApiException("결제 승인 실패", HttpStatus.BAD_REQUEST);
        }

        // 3. 저장 — 여기부터 실패하면 과금된 결제를 취소해야 한다.
        ReservationCompleteDTO dto;
        try {
            validateApprovedResult(request, result);
            dto = transactionTemplate.execute(status -> {
                // 객실 행 락을 트랜잭션의 "첫 문장"으로 잡는다. 과거에는 아래 중복 확인 SELECT 가 먼저 실행돼
                // MySQL REPEATABLE READ 스냅샷이 락 대기 전에 고정됐고, 락을 얻은 뒤의 재고 COUNT(일반 SELECT)가
                // 앞 트랜잭션이 막 커밋한 예약을 보지 못해 total_rooms 를 초과 예약할 수 있었다.
                // (재고 판정 자체도 ReserveService 에서 잠금 읽기로 하고, 운영 격리 수준은 READ COMMITTED 다.)
                roomRepository.findByIdForUpdate(reservationInfo.getRoomId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 객실입니다."));
                if (paymentRepository.existsByTransactionId(result.getPaymentKey())) {
                    throw new ApiException("이미 처리된 결제입니다.", HttpStatus.CONFLICT);
                }
                if (reservationRepository.existsByReservationNumber(reservationInfo.getReservationCode())) {
                    throw new ApiException("이미 처리된 예약번호입니다.", HttpStatus.CONFLICT);
                }

                // createReservation 이 (이미 잡은) 객실 행 락 안에서 재고를 잠금 읽기로 재확인한다.
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
        } catch (RuntimeException e) {
            throw compensate(request, result, e);
        }

        if (Boolean.TRUE.equals(result.getIsEmailSend()) && dto != null && dto.getGuestEmail() != null) {
            emailService.sendReservationConfirmation(dto.getGuestEmail(), dto);
            dto.setIsEmailSent(true);
        }

        return dto;
    }

    /**
     * 승인 후 저장 실패 시 보상: 토스 결제를 취소하고 사용자에게 돌려줄 예외를 만든다.
     * 같은 paymentKey 가 이미 저장돼 있으면(동시 중복 요청이 먼저 성공) 그 예약의 결제이므로 취소하지 않는다.
     */
    private ApiException compensate(TossConfirmRequest request, TossPaymentResult result, RuntimeException cause) {
        String paymentKey = result.getPaymentKey() != null ? result.getPaymentKey() : request.getPaymentKey();
        String orderId = request.getOrderId();

        if (paymentKey != null && isAlreadyPersisted(paymentKey)) {
            log.warn("승인된 결제가 이미 다른 요청으로 저장되어 취소하지 않음: orderId={}", orderId);
            return new ApiException("이미 처리된 결제입니다.", HttpStatus.CONFLICT);
        }

        log.error("결제 승인 후 예약 저장 실패, 결제 취소 시도: orderId={}, paymentKey={}, cause={}",
                orderId, paymentKey, cause.toString());
        try {
            tossService.cancelPayment(paymentKey, "예약 저장 실패로 인한 자동 취소");
        } catch (RuntimeException cancelError) {
            // 결제는 승인됐지만 예약이 없는 상태 — 수동 환불이 필요하다.
            log.error("[결제 보상 실패] 수동 환불 필요: orderId={}, paymentKey={}, cancelError={}",
                    orderId, paymentKey, cancelError.toString());
            return new ApiException(
                    "예약 처리 중 오류가 발생했고 결제 자동 취소에도 실패했습니다. 고객센터로 문의해 주세요. (주문번호: " + orderId + ")",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("결제 보상 취소 완료: orderId={}", orderId);
        boolean soldOut = cause instanceof ApiException api && api.getStatus() == HttpStatus.CONFLICT;
        String reason = soldOut ? cause.getMessage() : "예약 처리 중 오류가 발생했습니다.";
        return new ApiException(reason + " 결제는 자동으로 취소되었습니다.",
                soldOut ? HttpStatus.CONFLICT : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean isAlreadyPersisted(String paymentKey) {
        try {
            return paymentRepository.existsByTransactionId(paymentKey);
        } catch (RuntimeException e) {
            // 조회조차 실패하면 저장 여부를 알 수 없다. 이중 과금보다 취소가 안전하므로 취소를 진행한다.
            log.warn("결제 저장 여부 확인 실패: {}", e.toString());
            return false;
        }
    }

    /** 토스 호출 전 요청 자체의 일관성 검증. */
    private void validateRequest(TossConfirmRequest request) {
        ReservationRequestDTO reservationInfo = request.getReservationInfo();
        if (reservationInfo == null) {
            throw new IllegalArgumentException("예약 정보가 없습니다.");
        }
        if (reservationInfo.getReservationCode() == null || reservationInfo.getReservationCode().isBlank()) {
            throw new IllegalArgumentException("예약번호가 없습니다.");
        }
        if (!reservationInfo.getReservationCode().equals(request.getOrderId())) {
            throw new IllegalArgumentException("주문번호 불일치");
        }
        if (request.getAmount() == null) {
            throw new IllegalArgumentException("결제 금액이 없습니다.");
        }
    }

    /** 토스 승인 응답이 요청과 일치하는지 검증 (불일치 시 보상 대상). */
    private void validateApprovedResult(TossConfirmRequest request, TossPaymentResult result) {
        if (!request.getOrderId().equals(result.getOrderId())) {
            throw new IllegalArgumentException("주문번호 불일치");
        }
        if (result.getAmount() == null || request.getAmount().compareTo(result.getAmount()) != 0) {
            throw new IllegalArgumentException("결제 금액 불일치");
        }
    }

    private NormalizedAmount normalizeAndValidateAmount(Room room, ReservationRequestDTO request) {
        if (request.getRoomId() == null || request.getCheckIn() == null || request.getCheckOut() == null || request.getGuests() == null) {
            throw new IllegalArgumentException("예약 필수값이 누락되었습니다.");
        }

        // 지난 날짜(Asia/Seoul 기준)·역순·최대 숙박 일수 초과는 토스 승인 전에 400 으로 거절한다.
        StayDatePolicy.validate(request.getCheckIn(), request.getCheckOut());

        if (request.getGuests() <= 0 || request.getGuests() > room.getMaxOccupancy()) {
            throw new IllegalArgumentException("예약 인원이 허용 범위를 벗어났습니다.");
        }

        BigDecimal original = priceCalculator.calculate(room, request.getCheckIn(), request.getCheckOut(), request.getGuests()).total();
        BigDecimal couponDiscount = calculateCouponDiscount(room, original, request);
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

    private BigDecimal calculateCouponDiscount(Room room, BigDecimal original, ReservationRequestDTO request) {
        if (request.getUserCouponId() == null) {
            return BigDecimal.ZERO;
        }
        Long accommodationId = roomRepository.findAccommodationIdByRoomId(room.getRoomId()).orElse(null);
        // 소유·상태·기간·최소 금액·적용 대상·사용 이력 검증과 할인액(원 단위) 계산은 CouponService 가 담당한다.
        return couponService.calculateDiscountForPayment(
                request.getUserId(), request.getUserCouponId(), original, accommodationId);
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
