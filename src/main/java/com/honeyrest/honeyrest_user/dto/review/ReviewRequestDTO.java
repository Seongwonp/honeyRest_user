package com.honeyrest.honeyrest_user.dto.review;

import jakarta.validation.constraints.*;
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
public class ReviewRequestDTO {

    @NotNull
    private Long reservationId;

    @NotNull
    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal rating;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal cleanlinessRating;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal serviceRating;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal facilitiesRating;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal locationRating;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String content;

    @Size(max = 10)
    private List<String> imageUrls;
}
