package com.honeyrest.domain.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Review 답변/상태 변경: 다른 필드(createdAt·likeCount·본문)를 보존하고 허용 상태만 받는다. */
class ReviewTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2025, 3, 1, 10, 0);

    private static Review review(String status, String reply) throws Exception {
        Review review = Review.builder()
                .reviewId(1L)
                .accommodationId(10L)
                .roomId(20L)
                .rating(new BigDecimal("4.5"))
                .content("좋았어요")
                .reply(reply)
                .likeCount(7)
                .status(status)
                .build();
        // createdAt 은 BaseEntity(@CreationTimestamp) 가 채우므로 테스트에서는 리플렉션으로 넣는다
        Field createdAt = BaseEntity.class.getDeclaredField("createdAt");
        createdAt.setAccessible(true);
        createdAt.set(review, CREATED_AT);
        return review;
    }

    private static void assertUntouched(Review r) {
        assertThat(r.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(r.getLikeCount()).isEqualTo(7);
        assertThat(r.getContent()).isEqualTo("좋았어요");
        assertThat(r.getRating()).isEqualByComparingTo("4.5");
        assertThat(r.getReviewId()).isEqualTo(1L);
    }

    @Test
    void 답변_등록_수정_삭제는_reply_만_바꾼다() throws Exception {
        Review r = review("PUBLISHED", null);
        assertThat(r.hasReply()).isFalse();

        r.reply("감사합니다");
        assertThat(r.getReply()).isEqualTo("감사합니다");
        assertThat(r.hasReply()).isTrue();

        r.editReply("다시 감사드립니다");
        assertThat(r.getReply()).isEqualTo("다시 감사드립니다");

        r.deleteReply();
        assertThat(r.getReply()).isNull();
        assertThat(r.hasReply()).isFalse();

        r.deleteReply(); // 답변이 없어도 오류 없음
        assertThat(r.getStatus()).isEqualTo("PUBLISHED");
        assertUntouched(r);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void 빈_답변은_거부하고_기존_답변을_유지한다(String text) throws Exception {
        Review r = review("PUBLISHED", "기존 답변");
        assertThatThrownBy(() -> r.reply(text)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> r.editReply(text)).isInstanceOf(IllegalArgumentException.class);
        assertThat(r.getReply()).isEqualTo("기존 답변");
    }

    @ParameterizedTest
    @CsvSource({
            "PUBLISHED, PUBLISHED",
            "HIDDEN, HIDDEN",
            "DELETED, DELETED",
            "hidden, HIDDEN",
            "' Published ', PUBLISHED",
            "VISIBLE, PUBLISHED",
            "visible, PUBLISHED"
    })
    void 상태_변경은_정규화해서_저장하고_다른_필드는_보존한다(String input, String expected) throws Exception {
        Review r = review("HIDDEN", "답변");
        r.changeStatus(input);
        assertThat(r.getStatus()).isEqualTo(expected);
        assertThat(r.getReply()).isEqualTo("답변");
        assertUntouched(r);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ACTIVE", "PENDING", "REMOVED"})
    void 허용되지_않은_상태는_거부하고_기존_상태를_유지한다(String input) throws Exception {
        Review r = review("PUBLISHED", null);
        assertThatThrownBy(() -> r.changeStatus(input)).isInstanceOf(IllegalArgumentException.class);
        assertThat(r.getStatus()).isEqualTo("PUBLISHED");
    }
}
