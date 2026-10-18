package com.honeyrest.honeyrest_user.repository;

import com.honeyrest.domain.entity.Accommodation;
import com.honeyrest.domain.entity.Room;
import com.honeyrest.domain.entity.RoomImage;
import com.honeyrest.honeyrest_user.support.H2IntegrationTestSupport;
import com.honeyrest.honeyrest_user.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Room.images 연관관계(cascade) 가 두 앱의 실제 사용 방식과 충돌하지 않는지 확인한다 (H2).
 * - 객실을 지우면 이미지도 함께 지워진다 (room_image FK 로 객실 삭제가 실패하지 않음).
 * - 호스트 RoomServiceImpl.removeRoom 처럼 이미지를 먼저 일괄 삭제(+영속성 컨텍스트 clear)한 뒤 객실을 지워도 중복 삭제 오류가 없다.
 * - 호스트 updateRoom 처럼 빌더로 다시 만든 Room 을 merge 해도 이미지가 지워지지 않는다.
 * - 사용자 API 의 이미지 조회(RoomImage 를 room_id 로 조회)는 그대로 동작한다.
 */
@DisplayName("Room.images: 객실 삭제 시 이미지 연쇄 삭제 · 호스트 삭제/수정 흐름과 충돌 없음 (H2)")
class RoomImageCascadeH2Test extends H2IntegrationTestSupport {

    private Long roomWithImages(int count) {
        Accommodation acc = fixtures.accommodation(TestFixtures.unique("객실이미지시"), "ACTIVE");
        Room room = fixtures.room(acc.getAccommodationId(), 1, 2, "ACTIVE");
        // 호스트 RoomImageServiceImpl 처럼 RoomImage 를 따로 저장한다 (Room.images 컬렉션을 거치지 않음)
        transactionTemplate.executeWithoutResult(s -> {
            for (int i = 0; i < count; i++) {
                entityManager.persist(RoomImage.builder()
                        .room(entityManager.getReference(Room.class, room.getRoomId()))
                        .imageUrl("/uploads/room-" + room.getRoomId() + "-" + i + ".jpg")
                        .sortOrder(count - 1 - i)
                        .build());
            }
        });
        return room.getRoomId();
    }

    private long imageCount(Long roomId) {
        return transactionTemplate.execute(s -> entityManager.createQuery(
                        "select count(ri) from RoomImage ri where ri.room.roomId = :id", Long.class)
                .setParameter("id", roomId)
                .getSingleResult());
    }

    private boolean roomExists(Long roomId) {
        return transactionTemplate.execute(s -> entityManager.find(Room.class, roomId) != null);
    }

    @Test
    @DisplayName("객실을 지우면 이미지도 함께 지워진다")
    void deletingRoomCascadesToImages() {
        Long roomId = roomWithImages(2);
        assertThat(imageCount(roomId)).isEqualTo(2);

        transactionTemplate.executeWithoutResult(s -> entityManager.remove(entityManager.find(Room.class, roomId)));

        assertThat(roomExists(roomId)).isFalse();
        assertThat(imageCount(roomId)).isZero();
    }

    @Test
    @DisplayName("호스트 removeRoom 흐름: 이미지 일괄 삭제(clear) 후 객실 삭제해도 중복 삭제 오류가 없다")
    void hostStyleExplicitImageDeleteThenRoomDelete() {
        Long roomId = roomWithImages(2);

        transactionTemplate.executeWithoutResult(s -> {
            Room room = entityManager.find(Room.class, roomId);
            // ORoomImageRepository/RoomImageRepository.deleteByRoomRoomId (@Modifying clearAutomatically/flushAutomatically) 와 같다
            entityManager.flush();
            entityManager.createQuery("delete from RoomImage ri where ri.room.roomId = :id")
                    .setParameter("id", roomId)
                    .executeUpdate();
            entityManager.clear();
            // SimpleJpaRepository.delete(detached) = remove(merge(room))
            entityManager.remove(entityManager.merge(room));
        });

        assertThat(roomExists(roomId)).isFalse();
        assertThat(imageCount(roomId)).isZero();
    }

    @Test
    @DisplayName("호스트 updateRoom 흐름: 빌더로 다시 만든 Room 을 merge 해도 이미지가 남는다")
    void hostStyleBuilderMergeKeepsImages() {
        Long roomId = roomWithImages(3);

        transactionTemplate.executeWithoutResult(s -> {
            Room current = entityManager.find(Room.class, roomId);
            entityManager.merge(Room.builder()
                    .roomId(current.getRoomId())
                    .accommodation(current.getAccommodation())
                    .name(current.getName() + " (수정)")
                    .type(current.getType())
                    .price(new BigDecimal("150000.00"))
                    .maxOccupancy(current.getMaxOccupancy())
                    .standardOccupancy(current.getStandardOccupancy())
                    .totalRooms(current.getTotalRooms())
                    .status(current.getStatus())
                    .build());
        });

        assertThat(imageCount(roomId)).isEqualTo(3);
        assertThat(fixtures.find(Room.class, roomId).getPrice()).isEqualByComparingTo("150000.00");
    }

    @Test
    @DisplayName("Room.images 는 sortOrder 오름차순으로 읽힌다")
    void imagesAreOrderedBySortOrder() {
        Long roomId = roomWithImages(3);

        var orders = transactionTemplate.execute(s -> entityManager.find(Room.class, roomId).getImages().stream()
                .map(RoomImage::getSortOrder)
                .toList());

        assertThat(orders).containsExactly(0, 1, 2);
    }
}
