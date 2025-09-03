package com.honeyrest.honeyrest_user.dto.inquiry;

import com.honeyrest.honeyrest_user.entity.Inquiry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryListDTO {
    private Long inquiryId;
    private String title;
    private String content;
    private String accommodationName; //숙소명
    private Boolean isReplied; //  답변 여부
    private String createdAt;

    public static InquiryListDTO from(Inquiry inquiry) {
        return InquiryListDTO.builder()
                .inquiryId(inquiry.getInquiryId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .accommodationName(inquiry.getAccommodation().getName())
                .isReplied(inquiry.getIsReplied())
                .createdAt(inquiry.getCreatedAt().toString())
                .build();
    }
}
