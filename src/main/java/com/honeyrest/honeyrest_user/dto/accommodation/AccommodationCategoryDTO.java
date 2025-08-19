package com.honeyrest.honeyrest_user.dto.accommodation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationCategoryDTO {
    private Integer categoryId;
    private String name;
    private String iconUrl;
    private Integer sortOrder;
}