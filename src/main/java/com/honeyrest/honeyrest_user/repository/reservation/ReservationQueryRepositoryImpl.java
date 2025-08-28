package com.honeyrest.honeyrest_user.repository.reservation;

import com.honeyrest.honeyrest_user.entity.QReservation;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class ReservationQueryRepositoryImpl implements ReservationQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean isRoomReserved(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        QReservation reservation = QReservation.reservation;

        return queryFactory.selectOne()
                .from(reservation)
                .where(
                        reservation.room.roomId.eq(roomId),
                        reservation.checkInDate.lt(checkOut),
                        reservation.checkOutDate.gt(checkIn),
                        reservation.status.eq("CONFIRMED")
                )
                .fetchFirst() != null;
    }
}