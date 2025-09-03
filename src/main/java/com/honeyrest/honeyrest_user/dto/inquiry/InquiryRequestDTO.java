package com.honeyrest.honeyrest_user.dto.inquiry;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryRequestDTO {
    private Long userId;
    private Long accommodationId;
    private String category;
    private String title;
    private String content;
}
