package com.honeyrest.honeyrest_user.dto.reservation.guest;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuestReservationLookupRequestDTO {
    private String reservationCode;
    private String guestPhone;
    private String guestPassword;
}
