package com.honeyrest.honeyrest_user.repository.room;

import com.honeyrest.honeyrest_user.dto.room.RoomDetailDTO;

import java.time.LocalDate;

public interface RoomDetailQueryRepository {
    RoomDetailDTO fetchRoomDetailById(Long roomId, LocalDate checkIn, LocalDate checkOut, Integer guests);
}
