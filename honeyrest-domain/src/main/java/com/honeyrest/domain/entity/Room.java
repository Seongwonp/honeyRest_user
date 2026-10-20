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

    // 스키마(V1)가 LONGTEXT 라 매핑도 맞춘다. JSON 으로 두면 H2 에서 저장할 때마다 따옴표가 누적된다.
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
     * 객실 이미지. 객실 삭제 시 함께 삭제된다(cascade REMOVE). 연관관계 주인은 RoomImage.room 이다.
     * <p>
     * orphanRemoval 은 켜지 않는다. 호스트가 빌더로 새 Room 을 만들어 merge 하므로 켜면 수정할 때마다 이미지가 전부 지워진다.
     */
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC")
    private List<RoomImage> images = new ArrayList<>();

}
