package com.honeyrest.honeyrest_user.repository.reservation;

import java.time.LocalDate;

public interface ReservationQueryRepository {
    boolean isRoomReserved(Long roomId, LocalDate checkIn, LocalDate checkOut);
}
