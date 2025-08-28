package com.honeyrest.honeyrest_user.dto.room;

import com.honeyrest.honeyrest_user.dto.review.ReviewDTO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDetailDTO {
    private Long roomId;
    private String name;
    private String type;
    private BigDecimal price;
    private Integer maxOccupancy;
    private Integer standardOccupancy;
    private BigDecimal extraPersonFee;
    private String description;
    private boolean available;

    private List<String> images;         // RoomImage에서 추출
    private List<String> bedInfo;        // JSON → List<String>
    private List<String> amenities;      // JSON → List<String>

    private String accommodationName;
    private String accommodationAddress;
    private String accommodationThumbnail;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    private List<ReviewDTO> reviews;     // 리뷰 + 이미지 + 작성자
    private Long reviewCount; //리뷰 갯수

}
