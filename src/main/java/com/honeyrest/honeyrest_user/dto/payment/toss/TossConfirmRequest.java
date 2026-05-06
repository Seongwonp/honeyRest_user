package com.honeyrest.honeyrest_user.dto.payment.toss;

import com.honeyrest.honeyrest_user.dto.reservation.ReservationRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossConfirmRequest {

    @NotBlank
    private String paymentKey;

    @NotBlank
    private String orderId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    @Valid
    private ReservationRequestDTO reservationInfo;
}
