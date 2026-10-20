package com.honeyrest.honeyrest_user.service;

import com.honeyrest.domain.entity.Accommodation;
import com.honeyrest.domain.entity.Inquiry;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.security.JwtTokenProvider;
import com.honeyrest.honeyrest_user.support.H2IntegrationTestSupport;
import com.honeyrest.honeyrest_user.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 문의 수정/삭제 API: 소유권 검사(IDOR 방지)와 답변 보존 규칙을 두 사용자로 확인한다.
 */
@DisplayName("문의 API: 소유권(IDOR) · 답변/카테고리 보존 (H2)")
class InquiryOwnershipApiTest extends H2IntegrationTestSupport {

    @Autowired private TestRestTemplate rest;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User owner;
    private User attacker;
    private Accommodation accommodation;

    @BeforeEach
    void setUp() {
        owner = fixtures.user(0);
        attacker = fixtures.user(0);
        accommodation = fixtures.accommodation(TestFixtures.unique("문의시"), "ACTIVE");
    }

    private HttpHeaders auth(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtTokenProvider.createAccessToken(user));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ResponseEntity<String> edit(User as, Long inquiryId, Map<String, String> body) {
        return rest.exchange("/api/user/inquiries/edit/" + inquiryId, HttpMethod.PUT,
                new HttpEntity<>(body, auth(as)), String.class);
    }

    private ResponseEntity<String> delete(User as, Long inquiryId) {
        return rest.exchange("/api/user/inquiries/delete/" + inquiryId, HttpMethod.DELETE,
                new HttpEntity<>(auth(as)), String.class);
    }

    @Test
    @DisplayName("다른 사용자는 남의 문의를 수정·삭제할 수 없다 (403) 그리고 원본은 그대로다")
    void otherUserCannotEditOrDelete() {
        Inquiry inquiry = fixtures.inquiry(owner.getUserId(), accommodation.getAccommodationId(), "예약", null);

        ResponseEntity<String> editResponse = edit(attacker, inquiry.getInquiryId(),
                Map.of("title", "탈취", "content", "남의 문의 수정", "category", "기타"));
        ResponseEntity<String> deleteResponse = delete(attacker, inquiry.getInquiryId());

        assertThat(editResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Inquiry after = fixtures.find(Inquiry.class, inquiry.getInquiryId());
        assertThat(after).isNotNull();
        assertThat(after.getTitle()).isEqualTo("원래 제목");
    }

    @Test
    @DisplayName("작성자는 수정·삭제할 수 있고, 수정 시 카테고리가 반영되며 없는 문의는 404")
    void ownerCanEditAndDelete() {
        Inquiry inquiry = fixtures.inquiry(owner.getUserId(), accommodation.getAccommodationId(), "예약", null);

        ResponseEntity<String> editResponse = edit(owner, inquiry.getInquiryId(),
                Map.of("title", "새 제목", "content", "새 내용", "category", "결제"));
        assertThat(editResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Inquiry edited = fixtures.find(Inquiry.class, inquiry.getInquiryId());
        assertThat(edited.getTitle()).isEqualTo("새 제목");
        assertThat(edited.getCategory()).isEqualTo("결제");
        assertThat(edited.getAccommodation()).isNotNull();

        assertThat(delete(owner, inquiry.getInquiryId()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(fixtures.find(Inquiry.class, inquiry.getInquiryId())).isNull();
        assertThat(delete(owner, inquiry.getInquiryId()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("카테고리를 보내지 않으면 기존 카테고리를 유지한다")
    void categoryKeptWhenOmitted() {
        Inquiry inquiry = fixtures.inquiry(owner.getUserId(), accommodation.getAccommodationId(), "예약", null);

        edit(owner, inquiry.getInquiryId(), Map.of("title", "새 제목", "content", "새 내용"));

        assertThat(fixtures.find(Inquiry.class, inquiry.getInquiryId()).getCategory()).isEqualTo("예약");
    }

    @Test
    @DisplayName("답변이 달린 문의는 수정할 수 없고(409) 답변·카테고리가 지워지지 않는다")
    void repliedInquiryIsNotEditable() {
        Inquiry inquiry = fixtures.inquiry(owner.getUserId(), accommodation.getAccommodationId(), "예약", "답변드립니다.");

        ResponseEntity<String> response = edit(owner, inquiry.getInquiryId(),
                Map.of("title", "새 제목", "content", "새 내용", "category", "결제"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Inquiry after = fixtures.find(Inquiry.class, inquiry.getInquiryId());
        assertThat(after.getReply()).isEqualTo("답변드립니다.");
        assertThat(after.getIsReplied()).isTrue();
        assertThat(after.getCategory()).isEqualTo("예약");
        assertThat(after.getTitle()).isEqualTo("원래 제목");
    }
}
