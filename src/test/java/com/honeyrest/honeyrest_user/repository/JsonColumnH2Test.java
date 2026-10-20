package com.honeyrest.honeyrest_user.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.domain.entity.Accommodation;
import com.honeyrest.domain.entity.Company;
import com.honeyrest.domain.entity.Room;
import com.honeyrest.honeyrest_user.support.H2IntegrationTestSupport;
import com.honeyrest.honeyrest_user.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON 컬럼(room.bed_info/amenities, accommodation.amenities, company.bank_info)이 H2 에서
 * 저장할 때마다 JSON 문자열로 한 겹씩 감싸지지 않는지 확인한다 (매핑은 스키마와 같은 LONGTEXT).
 */
@DisplayName("JSON 컬럼: H2 에서 저장·재저장해도 JSON 텍스트가 그대로 유지된다")
class JsonColumnH2Test extends H2IntegrationTestSupport {

    private static final String BED_INFO = "[\"킹 베드 1개\",\"소파 베드\"]";
    private static final String AMENITIES = "[\"오션뷰\",\"욕조\"]";
    private static final String BANK_INFO = "{\"bank\":\"국민은행\",\"account\":\"123-456\"}";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String rawJson(String table, String column, String idColumn, Object id) {
        return transactionTemplate.execute(s -> (String) entityManager.createNativeQuery(
                        "select cast(" + column + " as varchar) from " + table + " where " + idColumn + " = ?1")
                .setParameter(1, id)
                .getSingleResult());
    }

    @Test
    @DisplayName("엔티티로 저장한 JSON 은 DB 에 JSON 텍스트로 들어가고, 더티 체킹 UPDATE 를 반복해도 바뀌지 않는다")
    void entityWriteKeepsJsonText() throws Exception {
        Accommodation acc = fixtures.accommodation(TestFixtures.unique("제이슨시"), "ACTIVE");
        Long accId = acc.getAccommodationId();
        Integer companyId = acc.getCompany().getCompanyId();

        // JSON 값을 엔티티 경로로 쓴다 (네이티브 UPDATE 로 넣으면 바인딩 문제를 재현할 수 없다)
        Long roomId = transactionTemplate.execute(s -> {
            Room room = Room.builder()
                    .accommodation(entityManager.getReference(Accommodation.class, accId))
                    .name(TestFixtures.unique("JSON 객실 "))
                    .price(new BigDecimal("100000.00"))
                    .maxOccupancy(2)
                    .standardOccupancy(2)
                    .totalRooms(1)
                    .status("ACTIVE")
                    .bedInfo(BED_INFO)
                    .amenities(AMENITIES)
                    .build();
            entityManager.persist(room);
            return room.getRoomId();
        });
        Integer jsonCompanyId = transactionTemplate.execute(s -> {
            Company company = Company.builder()
                    .name("JSON 업체")
                    .businessNumber(TestFixtures.unique("BNJ-"))
                    .ownerName("김업체")
                    .phone("02-000-0000")
                    .email("json@test.honeyrest")
                    .address("서울특별시 중구 1")
                    .bankInfo(BANK_INFO)
                    .status("APPROVED")
                    .build();
            entityManager.persist(company);
            return company.getCompanyId();
        });
        transactionTemplate.executeWithoutResult(s ->
                entityManager.createNativeQuery("update accommodation set amenities = ?1 format json where accommodation_id = ?2")
                        .setParameter(1, AMENITIES).setParameter(2, accId).executeUpdate());

        // 사용자 API 의 실제 쓰기 경로: 리뷰 작성/수정/삭제 시 Accommodation.updateRating(더티 체킹) — 여러 번 반복
        for (int i = 0; i < 3; i++) {
            BigDecimal rating = new BigDecimal("4." + i);
            transactionTemplate.executeWithoutResult(s ->
                    entityManager.find(Accommodation.class, accId).updateRating(rating));
        }

        assertThat(rawJson("room", "bed_info", "room_id", roomId)).isEqualTo(BED_INFO);
        assertThat(rawJson("room", "amenities", "room_id", roomId)).isEqualTo(AMENITIES);
        assertThat(rawJson("accommodation", "amenities", "accommodation_id", accId)).isEqualTo(AMENITIES);
        assertThat(rawJson("company", "bank_info", "company_id", jsonCompanyId)).isEqualTo(BANK_INFO);
        assertThat(rawJson("company", "bank_info", "company_id", companyId)).isNull();

        // 엔티티로 읽은 값도 사용자 화면(RoomDetailRepository 등)이 그대로 파싱할 수 있어야 한다
        Room room = fixtures.find(Room.class, roomId);
        assertThat(room.getBedInfo()).isEqualTo(BED_INFO);
        assertThat(objectMapper.readValue(room.getBedInfo(), new TypeReference<List<String>>() {}))
                .containsExactly("킹 베드 1개", "소파 베드");
        assertThat(fixtures.find(Accommodation.class, accId).getAmenities()).isEqualTo(AMENITIES);
        assertThat(fixtures.find(Company.class, jsonCompanyId).getBankInfo()).isEqualTo(BANK_INFO);
    }

    @Test
    @DisplayName("시드처럼 FORMAT JSON 으로 넣은 값을 엔티티로 읽고 다시 저장해도 그대로다")
    void seededJsonRoundTrip() {
        Accommodation acc = fixtures.accommodation(TestFixtures.unique("시드제이슨시"), "ACTIVE");
        Room room = fixtures.room(acc.getAccommodationId(), 1, 2, "ACTIVE");
        Long roomId = room.getRoomId();
        transactionTemplate.executeWithoutResult(s ->
                entityManager.createNativeQuery("update room set bed_info = ?1 format json where room_id = ?2")
                        .setParameter(1, BED_INFO).setParameter(2, roomId).executeUpdate());

        assertThat(fixtures.find(Room.class, roomId).getBedInfo()).isEqualTo(BED_INFO);

        // 객실 엔티티를 다시 쓰는 경로(호스트의 merge 갱신과 같음)
        transactionTemplate.executeWithoutResult(s -> {
            Room loaded = entityManager.find(Room.class, roomId);
            entityManager.merge(Room.builder()
                    .roomId(roomId)
                    .accommodation(loaded.getAccommodation())
                    .name(loaded.getName())
                    .type(loaded.getType())
                    .price(loaded.getPrice())
                    .maxOccupancy(loaded.getMaxOccupancy())
                    .standardOccupancy(loaded.getStandardOccupancy())
                    .totalRooms(loaded.getTotalRooms())
                    .status(loaded.getStatus())
                    .bedInfo(loaded.getBedInfo())
                    .build());
        });

        assertThat(rawJson("room", "bed_info", "room_id", roomId)).isEqualTo(BED_INFO);
        assertThat(fixtures.find(Room.class, roomId).getBedInfo()).isEqualTo(BED_INFO);
    }
}
