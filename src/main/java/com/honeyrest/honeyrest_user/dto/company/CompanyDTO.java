package com.honeyrest.honeyrest_user.dto.company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDTO {
    private Integer companyId;
    private String name;
    private String businessNumber;
    private String ownerName;
    private String phone;
    private String email;
    private String address;
}