package com.honeyrest.honeyrest_user.service.reservation;

import com.honeyrest.honeyrest_user.dto.reservation.PriceBreakdownDTO;
import com.honeyrest.domain.entity.PriceCalendar;
import com.honeyrest.domain.entity.Room;
import com.honeyrest.honeyrest_user.repository.room.PriceCalendarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PriceCalculator 테스트")
class PriceCalculatorTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 10, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 10, 4); // 3박

    private PriceCalendarRepository priceCalendarRepository;
    private PriceCalculator calculator;

    @BeforeEach
    void setUp() {
        priceCalendarRepository = mock(PriceCalendarRepository.class);
        calculator = new PriceCalculator(priceCalendarRepository);
    }

    private Room room(BigDecimal extraPersonFee) {
        return Room.builder()
                .roomId(10L)
                .price(BigDecimal.valueOf(100000))
                .standardOccupancy(2)
                .maxOccupancy(4)
                .extraPersonFee(extraPersonFee)
                .totalRooms(3)
                .build();
    }

    private void calendar(PriceCalendar... rows) {
        // 조회 구간은 체크인일 ~ 체크아웃 전날
        when(priceCalendarRepository.findByRoom_RoomIdAndDateBetween(10L, CHECK_IN, CHECK_OUT.minusDays(1)))
                .thenReturn(List.of(rows));
    }

    @Test
    @DisplayName("캘린더 행이 없으면 기본가 × 박수")
    void basePriceOnly() {
        calendar();

        PriceBreakdownDTO result = calculator.calculate(room(BigDecimal.valueOf(20000)), CHECK_IN, CHECK_OUT, 2);

        assertThat(result.nights()).isEqualTo(3);
        assertThat(result.nightlyPrices()).hasSize(3).allMatch(n -> !n.fromCalendar());
        assertThat(result.roomSubtotal()).isEqualByComparingTo("300000");
        assertThat(result.extraPersonCount()).isZero();
        assertThat(result.extraPersonTotal()).isEqualByComparingTo("0");
        assertThat(result.total()).isEqualByComparingTo("300000");
        verify(priceCalendarRepository).findByRoom_RoomIdAndDateBetween(10L, CHECK_IN, LocalDate.of(2026, 10, 3));
    }

    @Test
    @DisplayName("price_calendar 행이 있는 날짜는 캘린더 요금이 기본가보다 우선한다")
    void calendarOverridesBasePrice() {
        calendar(PriceCalendar.builder().date(LocalDate.of(2026, 10, 2)).price(BigDecimal.valueOf(150000)).build());

        PriceBreakdownDTO result = calculator.calculate(room(null), CHECK_IN, CHECK_OUT, 2);

        assertThat(result.nightlyPrices()).extracting(PriceBreakdownDTO.NightlyPrice::price)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(BigDecimal.valueOf(100000), BigDecimal.valueOf(150000), BigDecimal.valueOf(100000));
        assertThat(result.nightlyPrices().get(1).fromCalendar()).isTrue();
        assertThat(result.total()).isEqualByComparingTo("350000");
    }

    @Test
    @DisplayName("추가 인원 요금 = 1인 1박 요금 × 초과 인원 × 박수")
    void extraPersonFeeTimesNights() {
        calendar();

        // 기준 2인, 4인 예약 → 초과 2인, 20,000 × 2 × 3박 = 120,000
        PriceBreakdownDTO result = calculator.calculate(room(BigDecimal.valueOf(20000)), CHECK_IN, CHECK_OUT, 4);

        assertThat(result.extraPersonCount()).isEqualTo(2);
        assertThat(result.extraPersonTotal()).isEqualByComparingTo("120000");
        assertThat(result.total()).isEqualByComparingTo("420000");
    }

    @Test
    @DisplayName("extraPersonFee 가 null 이어도 NPE 없이 추가요금 0")
    void nullExtraPersonFee() {
        calendar();

        PriceBreakdownDTO result = calculator.calculate(room(null), CHECK_IN, CHECK_OUT, 4);

        assertThat(result.extraPersonCount()).isEqualTo(2);
        assertThat(result.extraPersonFeePerNight()).isEqualByComparingTo("0");
        assertThat(result.extraPersonTotal()).isEqualByComparingTo("0");
        assertThat(result.total()).isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("체크아웃이 체크인보다 빠르거나 같으면 예외")
    void invalidDates() {
        assertThatThrownBy(() -> calculator.calculate(room(null), CHECK_IN, CHECK_IN, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
