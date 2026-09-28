package com.honeyrest.honeyrest_user.repository.room;

import com.honeyrest.honeyrest_user.entity.PriceCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PriceCalendarRepository extends JpaRepository<PriceCalendar, Long> {

    /** 객실의 날짜별 요금 행 조회 (start, end 모두 포함). (room_id, date) 는 유니크. */
    List<PriceCalendar> findByRoom_RoomIdAndDateBetween(Long roomId, LocalDate start, LocalDate end);
}
