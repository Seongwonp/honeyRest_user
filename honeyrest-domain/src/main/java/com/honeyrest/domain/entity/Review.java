package com.honeyrest.domain.entity;


import com.honeyrest.domain.type.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "review", indexes = {
        @Index(name = "idx_review_user_id", columnList = "user_id"),
        @Index(name = "idx_review_reservation_id", columnList = "reservation_id"),
        @Index(name = "idx_review_accommodation_id", columnList = "accommodation_id"),
        @Index(name = "idx_review_status", columnList = "status")
})
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 중복 저장 컬럼 (FK 아님)
    @Column(name = "accommodation_id", nullable = false)
    private Long accommodationId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    // 평점 (0.00 ~ 5.00 등)
    @Column(name = "rating", precision = 3, scale = 2, nullable = false)
    private BigDecimal rating; // 종합 평점

    @Column(name = "cleanliness_rating", precision = 3, scale = 2)
    private BigDecimal cleanlinessRating; // 청결도 평점

    @Column(name = "service_rating", precision = 3, scale = 2)
    private BigDecimal serviceRating; // 서비스 평점

    @Column(name = "facilities_rating", precision = 3, scale = 2)
    private BigDecimal facilitiesRating; // 시설 평점

    @Column(name = "location_rating", precision = 3, scale = 2)
    private BigDecimal locationRating; // 위치 평점

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // 리뷰 내용

    @Column(name = "reply", columnDefinition = "TEXT")
    private String reply; // 업체/관리자 답변

    @Setter
    @Column(name = "like_count")
    private Integer likeCount; // 좋아요 수

    @Column(name = "status", length = 20, nullable = false)
    private String status; // 상태(ReviewStatus: PUBLISHED, HIDDEN, DELETED)

    /**
     * 사용자가 수정할 수 있는 필드(평점·본문)만 바꾼다.
     * 과거에는 빌더로 새 Review 를 만들어 save(merge) 했기 때문에 빌더에 넣지 않은
     * likeCount·reply 가 null 로 덮어써지고, 영속성 컨텍스트의 createdAt 도 null 이 됐다.
     * 관리 중인 엔티티를 직접 변경하면 createdAt/likeCount/reply/status 가 그대로 보존된다.
     */
    public void updateContent(BigDecimal rating,
                              BigDecimal cleanlinessRating,
                              BigDecimal serviceRating,
                              BigDecimal facilitiesRating,
                              BigDecimal locationRating,
                              String content) {
        this.rating = rating;
        this.cleanlinessRating = cleanlinessRating;
        this.serviceRating = serviceRating;
        this.facilitiesRating = facilitiesRating;
        this.locationRating = locationRating;
        this.content = content;
    }

    /**
     * 업체/관리자 답변을 등록한다. 이미 답변이 있으면 덮어쓴다(수정은 {@link #editReply(String)} 와 같다).
     * 호스트가 빌더로 Review 를 다시 만들어 merge 하던 방식은 빌더에 빠진 필드(createdAt·likeCount 등)를
     * null 로 덮어쓸 수 있으므로, 관리 중인 엔티티의 이 메서드로 reply 만 바꾼다.
     *
     * @throws IllegalArgumentException 답변이 null 이거나 공백뿐일 때 (답변 삭제는 {@link #deleteReply()})
     */
    public void reply(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("답변 내용이 비어 있습니다. 답변을 지우려면 deleteReply() 를 사용하세요.");
        }
        this.reply = text;
    }

    /** 답변 수정. {@link #reply(String)} 와 같은 규칙을 따른다. */
    public void editReply(String text) {
        reply(text);
    }

    /** 답변 삭제 (reply = null). 답변이 없어도 오류 없이 그대로 둔다. */
    public void deleteReply() {
        this.reply = null;
    }

    /** 답변이 있는지 */
    public boolean hasReply() {
        return reply != null && !reply.isBlank();
    }

    /**
     * 상태 변경. 허용 값은 {@link ReviewStatus#ALL} (PUBLISHED, HIDDEN, DELETED) 이며
     * 대소문자·앞뒤 공백을 무시하고, 과거 값 VISIBLE 은 PUBLISHED 로 저장한다.
     * 다른 필드(createdAt·likeCount·reply 등)는 건드리지 않는다.
     *
     * @throws IllegalArgumentException null·빈 값·허용되지 않은 상태
     */
    public void changeStatus(String status) {
        this.status = ReviewStatus.normalize(status);
    }

}
