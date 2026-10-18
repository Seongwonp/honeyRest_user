package com.honeyrest.honeyrest_user.repository.accommodation;

import com.honeyrest.domain.entity.Accommodation;
import com.honeyrest.domain.entity.Room;
import com.honeyrest.domain.type.ReservationStatus;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSearchDTO;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.service.accommodation.AccommodationService;
import com.honeyrest.honeyrest_user.support.H2IntegrationTestSupport;
import com.honeyrest.honeyrest_user.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** 검색: 비활성 숙소/객실 제외, 예약 가능 여부는 인원을 수용하는 운영 중 객실로만 판단 (H2). */
@DisplayName("숙소 검색: 상태 필터와 인원 기준 예약 가능 여부 (H2)")
class AccommodationSearchFilterH2Test extends H2IntegrationTestSupport {

    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(5);
    private static final LocalDate CHECK_OUT = LocalDate.now().plusDays(6);

    @Autowired private AccommodationService accommodationService;

    private Map<Long, AccommodationSearchDTO> search(String region, int guests) {
        PageResponseDTO<AccommodationSearchDTO> page = accommodationService.searchAvailable(
                region, null, null, CHECK_IN, CHECK_OUT, guests, null, "priceAsc",
                null, null, null, PageRequest.of(0, 20));
        return page.getContent().stream().collect(Collectors.toMap(AccommodationSearchDTO::getId, Function.identity()));
    }

    @Test
    @DisplayName("비활성 숙소, 운영 중인 객실이 없는 숙소는 검색되지 않는다")
    void inactiveAccommodationsAndRoomsAreExcluded() {
        String region = TestFixtures.unique("상태시");
        Accommodation active = fixtures.accommodation(region, "ACTIVE");
        fixtures.room(active.getAccommodationId(), 1, 2, "ACTIVE");
        Accommodation inactive = fixtures.accommodation(region, "INACTIVE");
        fixtures.room(inactive.getAccommodationId(), 1, 2, "ACTIVE");
        Accommodation onlyInactiveRooms = fixtures.accommodation(region, "ACTIVE");
        fixtures.room(onlyInactiveRooms.getAccommodationId(), 1, 2, "INACTIVE");
        Accommodation lowercaseActive = fixtures.accommodation(region, "active"); // 호스트가 소문자로 저장한 데이터
        fixtures.room(lowercaseActive.getAccommodationId(), 1, 2, "active");

        Map<Long, AccommodationSearchDTO> result = search(region, 2);

        assertThat(result.keySet()).containsExactlyInAnyOrder(active.getAccommodationId(), lowercaseActive.getAccommodationId());
    }

    @Test
    @DisplayName("인원을 수용하는 객실이 매진이면, 작은 객실·비활성 객실이 비어 있어도 예약 불가로 표시한다")
    void availabilityUsesOnlyEligibleRooms() {
        String region = TestFixtures.unique("재고시");
        Accommodation acc = fixtures.accommodation(region, "ACTIVE");
        Room family = fixtures.room(acc.getAccommodationId(), 1, 4, "ACTIVE");     // 4인 수용, 1실
        fixtures.room(acc.getAccommodationId(), 3, 2, "ACTIVE");                   // 2인실 (4인 검색에 부적합), 비어 있음
        fixtures.room(acc.getAccommodationId(), 3, 4, "INACTIVE");                 // 비활성, 비어 있음
        fixtures.reservation(null, family.getRoomId(), ReservationStatus.CONFIRMED, CHECK_IN, CHECK_OUT);

        Map<Long, AccommodationSearchDTO> fourGuests = search(region, 4);
        assertThat(fourGuests.get(acc.getAccommodationId()).isAvailable()).isFalse();

        // 2인 검색이면 비어 있는 2인실이 있으므로 예약 가능
        Map<Long, AccommodationSearchDTO> twoGuests = search(region, 2);
        assertThat(twoGuests.get(acc.getAccommodationId()).isAvailable()).isTrue();
    }
}
