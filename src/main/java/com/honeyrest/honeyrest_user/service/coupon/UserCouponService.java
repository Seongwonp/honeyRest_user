package com.honeyrest.honeyrest_user.service.coupon;

import com.honeyrest.honeyrest_user.dto.coupon.UserCouponDTO;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.entity.UserCoupon;
import com.honeyrest.honeyrest_user.repository.coupon.UserCouponRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCouponService {
    private final UserCouponRepository userCouponRepository;

    public PageResponseDTO<UserCouponDTO> getUserCoupons(Long userId, Pageable pageable) {
        Page<UserCoupon> page = userCouponRepository.findAllByUserUserId(userId, pageable);
        List<UserCouponDTO> content = page.getContent().stream()
                .map(UserCouponDTO::from)
                .collect(Collectors.toList());
        return PageResponseDTO.<UserCouponDTO>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}