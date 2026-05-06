package com.honeyrest.honeyrest_user.dto.reservation;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationRequestDTO {

    @NotNull
    private Long roomId;

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    @NotNull
    @Min(1)
    private Integer guests;

    @NotBlank
    @Size(max = 100)
    private String reservationCode;

    @NotBlank
    @Size(max = 50)
    private String guestName;

    @NotBlank
    @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 전화번호 형식이 아닙니다")
    private String guestPhone;

    @Size(max = 500)
    private String specialRequest;

    private Long couponId;
    private Long userId;

    private Boolean isEmailSend;

    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private String couponName;

    @Min(0)
    private Integer usedPoint;

}
