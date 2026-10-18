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
     * 예약 행을 FOR UPDATE 로 잠가 조회한다. 같은 예약에 대한 리뷰 동시 작성(더블 클릭 등)을 직렬화해
     * "예약당 리뷰 1건" 검사가 경쟁 상태에서도 지켜지게 한다. 반드시 트랜잭션 안에서 호출해야 한다.
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
     * {@link #countOverlapping} 의 잠금 읽기(locking read) 버전: 겹치는 예약 id 를 {@code SELECT ... FOR UPDATE} 로 읽는다.
     * <p>
     * 일반 SELECT 는 MySQL REPEATABLE READ 에서 트랜잭션의 첫 일관된 읽기 시점 스냅샷을 보므로, 객실 행 락을 기다리는 동안
     * 다른 트랜잭션이 커밋한 예약을 놓칠 수 있다. 잠금 읽기는 격리 수준과 상관없이 항상 최신 커밋 데이터를 읽는다.
     * 집계(COUNT) 에는 FOR UPDATE 를 붙일 수 없는 DB(H2)가 있어 id 목록을 읽고 개수는 호출자가 센다.
     * 객실 행 락(RoomRepository#findByIdForUpdate)을 먼저 잡은 트랜잭션 안에서만 호출한다.
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
