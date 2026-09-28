package com.honeyrest.honeyrest_user.service.reservation;

import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationDetailDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationSummaryDTO;
import com.honeyrest.honeyrest_user.dto.reservation.guest.GuestReservationLookupRequestDTO;
import com.honeyrest.honeyrest_user.entity.*;
import com.honeyrest.honeyrest_user.exception.ApiException;
import com.honeyrest.honeyrest_user.mapper.ReservationMapper;
import com.honeyrest.honeyrest_user.repository.payment.PaymentDetailRepository;
import com.honeyrest.honeyrest_user.repository.payment.PaymentRepository;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.review.ReviewRepository;
import com.honeyrest.honeyrest_user.repository.room.RoomRepository;
import com.honeyrest.honeyrest_user.service.coupon.CouponUsageService;
import com.honeyrest.honeyrest_user.service.PointHistoryService;
import com.honeyrest.honeyrest_user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Log4j2
@Service
@RequiredArgsConstructor
public class ReserveService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentDetailRepository paymentDetailRepository;
    private final ReservationMapper reservationMapper;
    private final ReviewRepository reviewRepository;

    private final UserService userService;
    private final CouponUsageService couponUsageService;
    private final PointHistoryService pointHistoryService;

    @Transactional
    public Reservation createReservation(ReservationRequestDTO request, BigDecimal amount, BigDecimal discountAmount) {
        // 객실 행을 FOR UPDATE 로 잠가 같은 객실의 동시 예약을 직렬화한 뒤 재고를 다시 확인한다.
        // (결제 승인 전 사전 확인은 락 없이 수행되므로 여기서의 재확인이 최종 판정이다.)
        Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 객실입니다"));
        assertRoomAvailable(room, request.getCheckIn(), request.getCheckOut());

        User user = request.getUserId() != null
                ? userRepository.findById(request.getUserId()).orElse(null)
                : null;

        Accommodation accommodation = room.getAccommodation();

        Reservation reservation = Reservation.builder()
                .room(room)
                .accommodation(accommodation)
                .roomName(room.getName())
                .checkInDate(request.getCheckIn())
                .checkOutDate(request.getCheckOut())
                .guestCount(request.getGuests())
                .guestName(request.getGuestName())
                .guestPhone(request.getGuestPhone())
                .specialRequest(request.getSpecialRequest())
                .user(user)
                .price(amount)
                // 서버가 PriceCalculator 로 다시 계산해 넣은 숙박 원가. 없으면(구 경로) 기본가로 대체.
                .originalPrice(request.getOriginalPrice() != null ? request.getOriginalPrice() : room.getPrice())
                .discountAmount(discountAmount)
                .reservationNumber(request.getReservationCode())
                .status(ReservationStatus.CONFIRMED)
                .build();

        reservationRepository.save(reservation);

        // 쿠폰 사용 처리
        if (request.getCouponId() != null) {
            couponUsageService.recordUsage(request.getCouponId(), reservation, discountAmount);
        }

        // 포인트 사용 처리
        if (request.getUsedPoint() != null && request.getUsedPoint() > 0) {
            userService.usePoint(request.getUserId(), request.getUsedPoint());
            pointHistoryService.recordUsage(request.getUserId(), reservation.getReservationId(), request.getUsedPoint());
        }

        return reservation;
    }

    /**
     * [checkIn, checkOut) 구간에 재고를 점유하는 예약 수가 객실 총 수(totalRooms) 이상이면 409 로 거절한다.
     * 락 없이 호출하면 사전 확인(빠른 실패) 용도이고, createReservation 안에서는 객실 행 락을 잡은 뒤 호출된다.
     */
    @Transactional(readOnly = true)
    public void assertRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("체크인/체크아웃 날짜가 올바르지 않습니다.");
        }
        int totalRooms = room.getTotalRooms() != null ? room.getTotalRooms() : 0;
        long occupied = reservationRepository.countOverlapping(
                room.getRoomId(), checkIn, checkOut, ReservationStatus.OCCUPYING);
        if (occupied >= totalRooms) {
            log.info("재고 부족으로 예약 거절: roomId={}, checkIn={}, checkOut={}, occupied={}, totalRooms={}",
                    room.getRoomId(), checkIn, checkOut, occupied, totalRooms);
            throw new ApiException("선택하신 날짜에 예약 가능한 객실이 없습니다.", HttpStatus.CONFLICT);
        }
    }


    @Transactional(readOnly = true)
    public ReservationCompleteDTO findGuestReservation(GuestReservationLookupRequestDTO request) {
        Reservation reservation = reservationRepository.findByReservationNumberAndGuestPhone(
                request.getReservationCode(), request.getGuestPhone()
        );
        if (reservation == null) {
            throw new IllegalArgumentException("예약 정보를 찾을 수 없습니다.");
        }
        Payment payment = paymentRepository.findByReservation(reservation).orElse(null);
        if (payment == null) {
            throw new IllegalArgumentException("결제 정보를 찾을 수 없습니다.");
        }
        return reservationMapper.toCompleteDTO(reservation, payment);
    }

    public PageResponseDTO<ReservationSummaryDTO> getUserReservationSummary(Long userId, Pageable pageable) {
        Page<Reservation> page = reservationRepository.findByUser_UserId(userId, pageable);

        List<ReservationSummaryDTO> content = page.stream()
                .map(reservationMapper::toSummaryDTO)
                .toList();

        return PageResponseDTO.<ReservationSummaryDTO>builder()
                .content(content)
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public ReservationDetailDTO getReservationDetail(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByReservationIdAndUser_UserId(reservationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        Payment payment = paymentRepository.findByReservation(reservation)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        PaymentDetail detail = paymentDetailRepository.findByPayment(payment)
                .orElseThrow(() -> new IllegalArgumentException("결제 상세 정보를 찾을 수 없습니다."));
        boolean isReviewed = reviewRepository.existsByReservation(reservation);
        ReservationDetailDTO dto = reservationMapper.toDetailDTO(reservation, payment, detail, isReviewed);

        // dto에는 결제 상세, 예약자 개인정보가 포함되어 있어 통째로 로깅하지 않는다.
        log.info("예약내역 조회: userId={}, reservationId={}", userId, reservationId);

        return dto;
    }


    public void requestReservationCancel(Long userId, Long reservationId, String reason) {
        Reservation reservation = reservationRepository.findByReservationIdAndUser_UserId(reservationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        if (!ReservationStatus.CONFIRMED.equals(reservation.getStatus())) {
            throw new IllegalStateException("확정된 예약만 취소 요청이 가능합니다.");
        }

        reservation.setStatus(ReservationStatus.CANCEL_REQUEST); // 상태 변경
        reservation.setCancelReason(reason);     // 사유 저장 (필드 추가 필요)

        reservationRepository.save(reservation);
        log.info("예약 취소 요청 접수됨: reservationId={}, reason={}", reservationId, reason);
    }


}