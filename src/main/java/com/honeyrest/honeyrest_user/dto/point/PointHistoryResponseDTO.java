package com.honeyrest.honeyrest_user.dto.point;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryResponseDTO {
    private int currentPoint;
    private List<PointHistoryDTO> history;
    private int totalPages;
    private long totalElements;
    private int page;
    private int size;
}
