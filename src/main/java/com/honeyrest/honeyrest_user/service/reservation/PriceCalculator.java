package com.honeyrest.honeyrest_user.service.reservation;

import com.honeyrest.honeyrest_user.dto.reservation.PriceBreakdownDTO;
import com.honeyrest.honeyrest_user.dto.reservation.PriceBreakdownDTO.NightlyPrice;
import com.honeyrest.honeyrest_user.entity.PriceCalendar;
import com.honeyrest.honeyrest_user.entity.Room;
import com.honeyrest.honeyrest_user.repository.room.PriceCalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 숙박 요금(원가) 계산의 단일 진입점.
 * 예약 폼 조회(ReserveInfoService)와 결제 승인 검증(PaymentOrchestrationService)이 반드시 같은 결과를 내도록
 * 요금 규칙은 이 클래스에만 둔다.
 *
 * <ul>
 *   <li>1박 요금: price_calendar 에 (room_id, date) 행이 있고 price 가 있으면 그 값, 없으면 room.price(기본가)</li>
 *   <li>숙박일: 체크인일 ~ 체크아웃 전날 (체크아웃 당일은 과금하지 않음)</li>
 *   <li>추가 인원 요금: extra_person_fee × max(0, 인원 - 기준 인원) × 숙박 일수 (fee 가 null 이면 0)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PriceCalculator {

    private final PriceCalendarRepository priceCalendarRepository;

    public PriceBreakdownDTO calculate(Room room, LocalDate checkIn, LocalDate checkOut, int guests) {
        Objects.requireNonNull(room, "room");
        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("체크인/체크아웃 날짜가 올바르지 않습니다.");
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        LocalDate lastNight = checkOut.minusDays(1);

        Map<LocalDate, BigDecimal> calendarPrices = priceCalendarRepository
                .findByRoom_RoomIdAndDateBetween(room.getRoomId(), checkIn, lastNight)
                .stream()
                .filter(pc -> pc.getDate() != null && pc.getPrice() != null)
                .collect(Collectors.toMap(PriceCalendar::getDate, PriceCalendar::getPrice, (a, b) -> a));

        List<NightlyPrice> nightly = new ArrayList<>((int) nights);
        BigDecimal roomSubtotal = BigDecimal.ZERO;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            BigDecimal calendarPrice = calendarPrices.get(d);
            BigDecimal price = calendarPrice != null ? calendarPrice : room.getPrice();
            if (price == null) {
                throw new IllegalStateException("객실 요금이 설정되지 않았습니다.");
            }
            nightly.add(new NightlyPrice(d, price, calendarPrice != null));
            roomSubtotal = roomSubtotal.add(price);
        }

        int standard = room.getStandardOccupancy() != null ? room.getStandardOccupancy() : guests;
        int extraCount = Math.max(0, guests - standard);
        BigDecimal feePerNight = room.getExtraPersonFee() != null ? room.getExtraPersonFee() : BigDecimal.ZERO;
        BigDecimal extraTotal = feePerNight
                .multiply(BigDecimal.valueOf(extraCount))
                .multiply(BigDecimal.valueOf(nights));

        return new PriceBreakdownDTO(
                nights,
                List.copyOf(nightly),
                roomSubtotal,
                extraCount,
                feePerNight,
                extraTotal,
                roomSubtotal.add(extraTotal)
        );
    }
}
