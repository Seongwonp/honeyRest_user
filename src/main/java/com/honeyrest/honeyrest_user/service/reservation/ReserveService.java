package com.honeyrest.honeyrest_user.service.reservation;

import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationDetailDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationSummaryDTO;
import com.honeyrest.honeyrest_user.dto.reservation.guest.GuestReservationLookupRequestDTO;
import com.honeyrest.honeyrest_user.entity.*;
import com.honeyrest.honeyrest_user.mapper.ReservationMapper;
import com.honeyrest.honeyrest_user.repository.payment.PaymentDetailRepository;
import com.honeyrest.honeyrest_user.repository.payment.PaymentRepository;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.review.ReviewRepository;
import com.honeyrest.honeyrest_user.repository.room.RoomRepository;
import com.honeyrest.honeyrest_user.service.CouponUsageService;
import com.honeyrest.honeyrest_user.service.PointHistoryService;
import com.honeyrest.honeyrest_user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
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
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 객실입니다"));

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
                .originalPrice(room.getPrice())
                .discountAmount(discountAmount)
                .reservationNumber(request.getReservationCode())
                .status("CONFIRMED")
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


    public ReservationCompleteDTO findGuestReservation(GuestReservationLookupRequestDTO request) {
        String reservationCode = request.getReservationCode()+"-"+request.getGuestPassword();
        Reservation reservation = reservationRepository.findByReservationNumberAndGuestPhone(
                reservationCode, request.getGuestPhone()
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

    public ReservationDetailDTO getReservationDetail(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByReservationIdAndUser_UserId(reservationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        Payment payment = paymentRepository.findByReservation(reservation)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        PaymentDetail detail = paymentDetailRepository.findByPayment(payment)
                .orElseThrow(() -> new IllegalArgumentException("결제 상세 정보를 찾을 수 없습니다."));
        boolean isReviewed = reviewRepository.existsByReservation(reservation);
        ReservationDetailDTO dto = reservationMapper.toDetailDTO(reservation, payment, detail, isReviewed);

        log.info("예약내역 조회: {}",dto);

        return dto;
    }


    public void requestReservationCancel(Long userId, Long reservationId, String reason) {
        Reservation reservation = reservationRepository.findByReservationIdAndUser_UserId(reservationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        if (!reservation.getStatus().equals("CONFIRMED")) {
            throw new IllegalStateException("확정된 예약만 취소 요청이 가능합니다.");
        }

        reservation.setStatus("CANCEL_REQUEST"); // 상태 변경
        reservation.setCancelReason(reason);     // 사유 저장 (필드 추가 필요)

        reservationRepository.save(reservation);
        log.info("예약 취소 요청 접수됨: reservationId={}, reason={}", reservationId, reason);
    }


}