package com.honeyrest.honeyrest_user.dto.inquiry;

import com.honeyrest.honeyrest_user.entity.Inquiry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InquiryDetailResponseDTO {
    private Long inquiryId;
    private Long userId;
    private Long accommodationId;
    private String accommodationName;   // 숙소명
    private String companyName;         // 답변 작성자(컴퍼니) 이름
    private String title;
    private String content;
    private String reply;
    private Boolean isReplied;
    private LocalDateTime createdAt;
    private LocalDateTime replyAt;      // 답변 시간

    public static InquiryDetailResponseDTO from(Inquiry inquiry){
        return InquiryDetailResponseDTO.builder()
                .inquiryId(inquiry.getInquiryId())
                .userId(inquiry.getUser().getUserId())
                .accommodationId(inquiry.getAccommodation().getAccommodationId())
                .accommodationName(inquiry.getAccommodation().getName())
                .companyName(inquiry.getAccommodation().getCompany().getName())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .reply(inquiry.getReply())
                .isReplied(inquiry.getIsReplied())
                .createdAt(inquiry.getCreatedAt())
                .replyAt(inquiry.getUpdatedAt())
                .build();


    }
}