package com.honeyrest.honeyrest_user.repository.room;

import com.honeyrest.domain.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * 예약 생성 시 객실 행을 비관적 쓰기 락(SELECT ... FOR UPDATE)으로 조회한다.
     * 같은 객실에 대한 동시 예약 트랜잭션을 직렬화해 재고 초과 예약을 막는다.
     * 반드시 트랜잭션 안에서 호출해야 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.roomId = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);

    /** 객실의 숙소 id (지연 로딩 프록시를 트랜잭션 밖에서 초기화하지 않도록 스칼라로 조회). */
    @Query("select r.accommodation.accommodationId from Room r where r.roomId = :roomId")
    Optional<Long> findAccommodationIdByRoomId(@Param("roomId") Long roomId);
}
