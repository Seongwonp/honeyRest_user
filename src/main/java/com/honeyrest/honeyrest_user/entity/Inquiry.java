package com.honeyrest.honeyrest_user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inquiry", indexes = {
        @Index(name = "idx_inquiry_user_id", columnList = "user_id"),
        @Index(name = "idx_inquiry_accommodation_id", columnList = "accommodation_id"),
        @Index(name = "idx_inquiry_is_replied", columnList = "is_replied")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inquiryId;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 관련 숙소
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id")
    private Accommodation accommodation;

    // 제목
    private String title;

    // 문의 내용
    @Column(columnDefinition = "TEXT")
    private String content;

    // 답변 내용
    private String reply;

    // 답변 여부
    private Boolean isReplied;

    // 문의 카테고리
    @Column(length = 50)
    private String category;
}