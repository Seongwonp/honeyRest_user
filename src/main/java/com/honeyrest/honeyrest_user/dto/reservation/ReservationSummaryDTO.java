package com.honeyrest.honeyrest_user.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationSummaryDTO {
    private Long reservationId;
    private String thumbnailUrl;
    private String reservationCode;
    private String accommodationName;
    private String roomName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private String status; // 예약 상태 (CONFIRMED, CANCELLED 등)
    private BigDecimal price;
}
