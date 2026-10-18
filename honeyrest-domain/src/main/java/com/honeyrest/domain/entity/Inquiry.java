package com.honeyrest.domain.entity;

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

    /* ===== 도메인 변경 메서드들 (호스트 사본에서 합침) ===== */
    public void changeTitle(String title)      { this.title = title; }
    public void changeContent(String content)  { this.content = content; }
    public void changeCategory(String category){ this.category = category; }

    /** 답변 등록/수정 시 isReplied 동기화 */
    public void changeReply(String reply) {
        this.reply = reply;
        this.isReplied = (reply != null && !reply.isBlank());
    }
    /** 답변이 달렸는지 (reply 내용 기준. isReplied 플래그만 true 인 과거 데이터도 답변 있음으로 본다). */
    public boolean hasReply() {
        return (reply != null && !reply.isBlank()) || Boolean.TRUE.equals(isReplied);
    }

    /**
     * 작성자의 문의 수정. 관리 중인 엔티티의 제목·내용·카테고리만 바꾸고 답변(reply/isReplied)·숙소·작성자는 보존한다.
     * 답변이 달린 문의는 수정할 수 없다 (프론트 InquiryEditModal 의 isEditable = !inquiry.reply 와 같은 규칙).
     * category 가 null/빈 값이면 기존 카테고리를 유지한다.
     *
     * @throws IllegalStateException 이미 답변이 달린 문의
     */
    public void editQuestion(String title, String content, String category) {
        if (hasReply()) {
            throw new IllegalStateException("답변이 등록된 문의는 수정할 수 없습니다.");
        }
        this.title = title;
        this.content = content;
        if (category != null && !category.isBlank()) {
            this.category = category;
        }
    }

    public void changeUser(User user) { this.user = user; }
    public void changeAccommodation(Accommodation acc) { this.accommodation = acc; }
}