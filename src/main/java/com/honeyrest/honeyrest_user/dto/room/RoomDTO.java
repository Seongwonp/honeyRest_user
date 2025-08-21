package com.honeyrest.honeyrest_user.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    private Long roomId;
    private String name;
    private String type;
    private BigDecimal price;
    private Integer maxOccupancy;
    private Integer standardOccupancy;
    private BigDecimal extraPersonFee;
    private List<String> bedInfo;
    private List<String> amenities;
    private String description;
    private Integer totalRooms;
    private String status;
    private boolean available;
    private List<String> images;
}