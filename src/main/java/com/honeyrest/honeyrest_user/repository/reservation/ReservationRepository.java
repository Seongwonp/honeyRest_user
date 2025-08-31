package com.honeyrest.honeyrest_user.repository.reservation;

import com.honeyrest.honeyrest_user.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Reservation findByReservationNumberAndGuestPhone(String reservationNumber, String guestPhone);
    Page<Reservation> findByUser_UserId(Long userId, Pageable pageable);
    Optional<Reservation> findByReservationIdAndUser_UserId(Long reservationId, Long userId);
}
