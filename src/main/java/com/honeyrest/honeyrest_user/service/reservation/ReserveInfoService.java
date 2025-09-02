package com.honeyrest.honeyrest_user.service.reservation;

import com.honeyrest.honeyrest_user.dto.CancellationPolicyDTO;
import com.honeyrest.honeyrest_user.dto.coupon.AvailableCouponDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationFormInfoDTO;
import com.honeyrest.honeyrest_user.entity.Accommodation;
import com.honeyrest.honeyrest_user.entity.Room;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.room.RoomRepository;
import com.honeyrest.honeyrest_user.service.CancellationPolicyService;
import com.honeyrest.honeyrest_user.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReserveInfoService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final CancellationPolicyService cancellationPolicyService;
    private final CouponService couponService;

    public ReservationFormInfoDTO getFormInfo(Long roomId, Long userId, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("체크인/체크아웃 날짜가 올바르지 않습니다");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("객실 정보를 찾을 수 없습니다"));

        if (guests == null || guests <= 0 || guests > room.getMaxOccupancy()) {
            throw new IllegalArgumentException("예약 인원이 허용 범위를 초과했습니다");
        }

        Accommodation acc = room.getAccommodation();

        String userName = null;
        String userPhone = null;
        Integer userPoint =  null;
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다"));
            userName = user.getName();
            userPhone = user.getPhone();
            userPoint = user.getPoint();
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal basePrice = room.getPrice().multiply(BigDecimal.valueOf(nights));

        BigDecimal extraFee = BigDecimal.ZERO;
        if (guests > room.getStandardOccupancy()) {
            int extraCount = guests - room.getStandardOccupancy();
            extraFee = room.getExtraPersonFee().multiply(BigDecimal.valueOf(extraCount));
        }

        BigDecimal originalPrice = basePrice.add(extraFee);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal finalPrice = originalPrice.subtract(discount);

        List<CancellationPolicyDTO> cancellationPolicies = cancellationPolicyService.getCancellationPoliciesByAccommodationId(acc.getAccommodationId());

        List<AvailableCouponDTO> availableCoupons = (userId != null)
                ? couponService.getAvailableCoupons(userId, originalPrice, acc.getAccommodationId())
                : Collections.emptyList();

        ReservationFormInfoDTO formInfoDTO = ReservationFormInfoDTO.builder()
                .roomId(room.getRoomId())
                .roomName(room.getName())
                .price(room.getPrice())
                .standardOccupancy(room.getStandardOccupancy())
                .maxOccupancy(room.getMaxOccupancy())
                .accommodationName(acc.getName())
                .accommodationThumbnail(acc.getThumbnail())
                .accommodationAddress(acc.getAddress())
                .cancellationPolicy(cancellationPolicies)
                .userId(userId)
                .userName(userName)
                .userPhone(userPhone)
                .availablePoints(userPoint)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(guests)
                .nights(nights)
                .originalPrice(originalPrice)
                .discountAmount(discount)
                .finalPrice(finalPrice)
                .availableCoupons(availableCoupons)
                .build();

        log.info("!! 예약 진행 정보 = {}", formInfoDTO);
        return formInfoDTO;
    }
}