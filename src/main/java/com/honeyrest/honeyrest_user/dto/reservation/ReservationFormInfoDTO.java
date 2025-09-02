package com.honeyrest.honeyrest_user.dto.reservation;

import com.honeyrest.honeyrest_user.dto.CancellationPolicyDTO;
import com.honeyrest.honeyrest_user.dto.coupon.AvailableCouponDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationFormInfoDTO {
    private Long roomId;
    private String roomName;
    private BigDecimal price;
    private Integer standardOccupancy;
    private Integer maxOccupancy;

    private String accommodationName;
    private String accommodationThumbnail;
    private String accommodationAddress;
    private List<CancellationPolicyDTO> cancellationPolicy;

    private Long userId;
    private String userName;
    private String userPhone;
    private Integer availablePoints;

    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private Long nights;

    private List<AvailableCouponDTO> availableCoupons;

    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
}
