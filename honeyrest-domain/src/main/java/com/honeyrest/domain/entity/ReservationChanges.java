package com.honeyrest.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@link Reservation#update} 에 넘기는 부분 변경 값.
 * <p>
 * 호스트 사본의 {@code Reservation.update(ReservationDTO, ...)} 는 호스트 DTO 에 직접 의존해서 공유 모듈로 옮길 수 없었다.
 * 필요한 getter 만 이 인터페이스로 뽑아, 호스트 {@code ReservationDTO} 가 구현하도록 했다(Lombok getter 이름이 같다).
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
