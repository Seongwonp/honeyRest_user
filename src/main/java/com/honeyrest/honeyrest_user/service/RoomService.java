package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.room.RoomDetailDTO;
import com.honeyrest.honeyrest_user.repository.room.RoomDetailQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Log4j2
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomDetailQueryRepository roomDetailQueryRepository;

    public RoomDetailDTO getRoomDetail(Long roomId, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        return roomDetailQueryRepository.fetchRoomDetailById(roomId, checkIn, checkOut, guests);
    }

}
