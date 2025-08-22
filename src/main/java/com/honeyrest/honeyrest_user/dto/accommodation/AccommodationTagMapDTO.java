package com.honeyrest.honeyrest_user.dto.accommodation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationTagMapDTO {
    private Long mapId;
    private Long accommodationId;
    private Long tagId;
    private String tagName;
    private String tagCategory;
    private String iconName;
}