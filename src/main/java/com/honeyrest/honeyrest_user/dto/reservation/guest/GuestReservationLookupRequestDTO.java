package com.honeyrest.honeyrest_user.dto.reservation.guest;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuestReservationLookupRequestDTO {

    @NotBlank
    private String reservationCode;

    @NotBlank
    private String guestPhone;

    @NotBlank
    private String guestPassword;
}
