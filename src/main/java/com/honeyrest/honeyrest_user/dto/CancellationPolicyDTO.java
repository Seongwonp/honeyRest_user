package com.honeyrest.honeyrest_user.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationPolicyDTO {

    private Long policyId;
    private String policyName;
    private String detail;
    private Long accommodationId; // 필요시
}