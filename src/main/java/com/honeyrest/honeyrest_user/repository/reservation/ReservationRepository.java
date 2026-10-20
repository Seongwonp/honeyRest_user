package com.honeyrest.honeyrest_user.repository.reservation;

import com.honeyrest.domain.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Reservation findByReservationNumberAndGuestPhone(String reservationNumber, String guestPhone);
    Page<Reservation> findByUser_UserId(Long userId, Pageable pageable);
    Optional<Reservation> findByReservationIdAndUser_UserId(Long reservationId, Long userId);
    boolean existsByReservationNumber(String reservationNumber);

    /**
     * 예약 행을 FOR UPDATE 로 잠가 조회한다. 리뷰 동시 작성을 직렬화한다. 트랜잭션 안에서 호출해야 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.reservationId = :reservationId")
    Optional<Reservation> findByIdForUpdate(@Param("reservationId") Long reservationId);

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

    /**
     * {@link #countOverlapping} 의 잠금 읽기 버전. 겹치는 예약 id 를 {@code SELECT ... FOR UPDATE} 로 읽는다.
     * 일반 SELECT 는 REPEATABLE READ 스냅샷이라 락 대기 중 커밋된 예약을 놓칠 수 있다.
     * H2 는 COUNT 에 FOR UPDATE 를 붙일 수 없어 id 목록을 읽는다. 객실 행 락을 잡은 트랜잭션 안에서만 호출한다.
     */
    @Query(value = """
            select r.reservation_id from reservation r
             where r.room_id = :roomId
               and r.status in (:statuses)
               and r.check_in_date < :checkOut
               and r.check_out_date > :checkIn
               for update
            """, nativeQuery = true)
    List<Long> findOverlappingIdsForUpdate(@Param("roomId") Long roomId,
                                           @Param("checkIn") LocalDate checkIn,
                                           @Param("checkOut") LocalDate checkOut,
                                           @Param("statuses") Collection<String> statuses);
}
