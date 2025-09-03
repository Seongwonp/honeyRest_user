package com.honeyrest.honeyrest_user.dto.inquiry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryCountDTO {
    private long totalCount;
    private long answeredCount;
    private long unansweredCount;
}
