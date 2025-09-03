package com.honeyrest.honeyrest_user.dto.coupon;

import com.honeyrest.honeyrest_user.entity.UserCoupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCouponDTO {
    private Long userCouponId;
    private String name;
    private String code;
    private String discountType;
    private String discountValue;
    private String validFrom;
    private String validTo;
    private boolean used;

    public static UserCouponDTO from(UserCoupon userCoupon) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return UserCouponDTO.builder()
                .userCouponId(userCoupon.getUserCouponId())
                .name(userCoupon.getCoupon().getName())
                .code(userCoupon.getCoupon().getCode())
                .discountType(userCoupon.getCoupon().getDiscountType())
                .discountValue(userCoupon.getCoupon().getDiscountValue().toPlainString())
                .validFrom(userCoupon.getIssuedAt().format(formatter))
                .validTo(userCoupon.getExpiredAt().format(formatter))
                .used("USED".equals(userCoupon.getStatus()))
                .build();
    }
}