package com.honeyrest.honeyrest_user.mapper;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationDetailDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationSummaryDTO;
import com.honeyrest.honeyrest_user.entity.Payment;
import com.honeyrest.honeyrest_user.entity.PaymentDetail;
import com.honeyrest.honeyrest_user.entity.Reservation;
import com.honeyrest.honeyrest_user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationMapper {

    private final PaymentDetailMapper paymentDetailMapper;

    public ReservationCompleteDTO toCompleteDTO(Reservation reservation, Payment payment) {


        return ReservationCompleteDTO.builder()
                .reservationId(reservation.getReservationId())
                .reservationCode(reservation.getReservationNumber())
                .accommodationName(reservation.getRoom().getAccommodation().getName())
                .roomName(reservation.getRoom().getName())
                .checkIn(reservation.getCheckInDate())
                .checkOut(reservation.getCheckOutDate())
                .guests(reservation.getGuestCount())
                .guestName(reservation.getGuestName())
                .guestPhone(reservation.getGuestPhone())
                .paymentMethod(payment != null ? payment.getPaymentMethod() : null)
                .paymentStatus(payment != null ? payment.getPaymentStatus() : null)
                .receiptUrl(payment != null ? payment.getReceiptUrl() : null)
                .originalPrice(reservation.getOriginalPrice())
                .discountAmount(reservation.getDiscountAmount())
                .finalPrice(reservation.getPrice())
                .remainingPoint(reservation.getUser() != null ? reservation.getUser().getPoint() : null)
                .build();
    }

    public ReservationSummaryDTO toSummaryDTO(Reservation r) {
        return ReservationSummaryDTO.builder()
                .reservationId(r.getReservationId())
                .thumbnailUrl(r.getAccommodation().getThumbnail())
                .reservationCode(r.getReservationNumber())
                .accommodationName(r.getRoom().getAccommodation().getName())
                .roomName(r.getRoomName())
                .checkIn(r.getCheckInDate())
                .checkOut(r.getCheckOutDate())
                .guests(r.getGuestCount())
                .status(r.getStatus())
                .price(r.getPrice())
                .build();
    }

    public ReservationDetailDTO toDetailDTO(Reservation reservation, Payment payment, PaymentDetail detail, Boolean isReviewed) {
        return ReservationDetailDTO.builder()
                // 예약 정보
                .reservationId(reservation.getReservationId())
                .thumbnailUrl(reservation.getAccommodation().getThumbnail())
                .reservationCode(reservation.getReservationNumber())
                .accommodationId(reservation.getAccommodation().getAccommodationId())
                .accommodationName(reservation.getRoom().getAccommodation().getName())
                .roomName(reservation.getRoomName())
                .checkIn(reservation.getCheckInDate())
                .checkOut(reservation.getCheckOutDate())
                .guests(reservation.getGuestCount())
                .status(reservation.getStatus())
                .isReviewed(isReviewed)

                // 사용자 정보
                .guestName(reservation.getGuestName())
                .guestPhone(reservation.getGuestPhone())

                // 결제 정보
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .originalPrice(reservation.getOriginalPrice())       // 할인 전 금액
                .discountAmount(reservation.getDiscountAmount())     // 할인 금액
                .finalPrice(reservation.getPrice())                  // 실제 결제 금액
                .receiptUrl(payment.getReceiptUrl())

                // 결제 상세 정보
                .paymentDetailDTO(paymentDetailMapper.toDTO(detail))

                .build();
    }

}