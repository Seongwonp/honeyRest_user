package com.honeyrest.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@link Reservation#update} 에 넘기는 부분 변경 값. 호스트 ReservationDTO 가 구현한다.
 * null 인 값은 "변경하지 않음" 을 뜻한다.
 */
public interface ReservationChanges {

    LocalDate getCheckInDate();

    LocalDate getCheckOutDate();

    Integer getGuestCount();

    String getGuestName();

    String getGuestPhone();

    BigDecimal getPrice();

    String getStatus();

    String getCancelReason();

    String getSpecialRequest();

    String getAccommodationName();
}
