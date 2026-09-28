package com.honeyrest.honeyrest_user.dto.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 숙박 요금 계산 결과 (할인·포인트 적용 전 원가).
 *
 * @param nights                 숙박 일수
 * @param nightlyPrices          숙박일별 1박 요금 (체크인일 ~ 체크아웃 전날)
 * @param roomSubtotal           1박 요금 합계
 * @param extraPersonCount       기준 인원 초과 인원 수
 * @param extraPersonFeePerNight 초과 1인당 1박 추가요금 (미설정 시 0)
 * @param extraPersonTotal       추가요금 합계 = 1인 1박 요금 × 초과 인원 × 숙박 일수
 * @param total                  roomSubtotal + extraPersonTotal
 */
public record PriceBreakdownDTO(
        long nights,
        List<NightlyPrice> nightlyPrices,
        BigDecimal roomSubtotal,
        int extraPersonCount,
        BigDecimal extraPersonFeePerNight,
        BigDecimal extraPersonTotal,
        BigDecimal total
) {

    /**
     * @param date            숙박일
     * @param price           해당 날짜 1박 요금
     * @param fromCalendar    price_calendar 요금이 적용되었으면 true, room.price(기본가)면 false
     */
    public record NightlyPrice(LocalDate date, BigDecimal price, boolean fromCalendar) {
    }
}
