package com.honeyrest.honeyrest_user.mapper;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationCompleteDTO;
import com.honeyrest.honeyrest_user.entity.Payment;
import com.honeyrest.honeyrest_user.entity.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {
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
                .build();
    }
}