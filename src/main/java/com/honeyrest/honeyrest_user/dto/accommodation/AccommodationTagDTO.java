package com.honeyrest.honeyrest_user.dto.accommodation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationTagDTO {
    private Long tagId;
    private String name;       // 태그명 (예: 바다뷰, 바베큐)
    private String category;   // 태그 카테고리 (예: 테마, 시설, 뷰 등)
    private String iconName;   // 아이콘 이름 (예: FaUmbrellaBeach)
}