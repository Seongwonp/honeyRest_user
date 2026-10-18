package com.honeyrest.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "room")
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation; // 숙소 ID

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "type", length = 60)
    private String type;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "max_occupancy", nullable = false)
    private Integer maxOccupancy;

    @Column(name = "standard_occupancy", nullable = false)
    private Integer standardOccupancy;

    @Column(name = "extra_person_fee", precision = 10, scale = 2)
    private BigDecimal extraPersonFee;

    // JSON 텍스트를 담는 문자열 컬럼. 실제 스키마(Flyway V1)는 LONGTEXT 이므로 매핑도 LONGTEXT 로 맞춘다.
    // 과거 columnDefinition = "JSON" 이면 H2(demo/e2e/test, ddl-auto=create-drop)가 JSON 타입 컬럼을 만들고,
    // 문자열 파라미터를 JSON '문자열 값'으로 감싸 저장해 저장할 때마다 따옴표가 누적됐다('["a"]' → "\"[...]\"").
    // MySQL 은 Flyway 가 스키마를 관리하므로(ddl-auto=validate) 이 값은 H2 DDL 에만 영향을 준다.
    @Column(name = "bed_info", columnDefinition = "LONGTEXT")
    private String bedInfo;

    @Column(name = "amenities", columnDefinition = "LONGTEXT")
    private String amenities;

    @Column(name = "description",columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_rooms", nullable = false)
    private Integer totalRooms;

    @Column(name = "status" , length = 20)
    private String status;

    /**
     * 객실 이미지. 객실을 지우면(em.remove) 이미지도 함께 지워진다(cascade REMOVE) — room_image FK 때문에 객실 삭제가 실패하지 않는다.
     * 연관관계의 주인은 RoomImage.room 이다. 두 앱 모두 이미지를 RoomImage 리포지토리로 직접 저장/삭제하며 이 컬렉션은 읽기·연쇄 삭제용이다.
     * <p>
     * orphanRemoval 은 일부러 켜지 않는다: 호스트 RoomServiceImpl.updateRoom/ORoomServiceImpl 은 빌더로 새 Room 을 만들어
     * save(merge) 하는데, 빌더의 images 는 빈 리스트라 orphanRemoval 이 켜져 있으면 객실을 수정할 때마다 이미지가 전부 삭제된다
     * (RoomImageCascadeH2Test 로 확인). 호스트가 관리 엔티티를 직접 변경하는 방식으로 바뀌면 그때 orphanRemoval 을 검토한다.
     * 호스트 removeRoom 의 이미지 선삭제(deleteByRoomRoomId) 는 그대로 두어도 중복 삭제가 일어나지 않는다.
     */
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC")
    private List<RoomImage> images = new ArrayList<>();

}
