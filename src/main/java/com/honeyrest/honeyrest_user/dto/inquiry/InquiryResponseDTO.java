package com.honeyrest.honeyrest_user.dto.inquiry;

import com.honeyrest.honeyrest_user.entity.Inquiry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryResponseDTO {
    private Long inquiryId;
    private Long userId;
    private Long accommodationId;
    private String title;
    private String content;
    private String reply;
    private Boolean isReplied;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InquiryResponseDTO from(Inquiry inquiry) {
        return InquiryResponseDTO.builder()
                .inquiryId(inquiry.getInquiryId())
                .userId(inquiry.getUser().getUserId())
                .accommodationId(inquiry.getAccommodation().getAccommodationId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .reply(inquiry.getReply())
                .isReplied(inquiry.getIsReplied())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
