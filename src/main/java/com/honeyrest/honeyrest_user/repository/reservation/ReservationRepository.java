package com.honeyrest.honeyrest_user.repository.reservation;

import com.honeyrest.honeyrest_user.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Reservation findByReservationNumberAndGuestPhone(String reservationNumber, String guestPhone);
    Page<Reservation> findByUser_UserId(Long userId, Pageable pageable);
    Optional<Reservation> findByReservationIdAndUser_UserId(Long reservationId, Long userId);
    boolean existsByReservationNumber(String reservationNumber);

    /**
     * [checkIn, checkOut) 구간과 숙박일이 겹치는 예약 수. 체크아웃 당일은 점유하지 않는다.
     * 예약 1건 = 객실 1개로 계산한다 (수량 컬럼 없음).
     */
    @Query("""
            select count(r) from Reservation r
             where r.room.roomId = :roomId
               and r.status in :statuses
               and r.checkInDate < :checkOut
               and r.checkOutDate > :checkIn
            """)
    long countOverlapping(@Param("roomId") Long roomId,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut,
                          @Param("statuses") Collection<String> statuses);
}
