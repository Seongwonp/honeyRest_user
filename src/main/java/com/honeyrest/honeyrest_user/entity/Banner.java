package com.honeyrest.honeyrest_user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "banner")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bannerId;

    private String title;

    private String imageUrl;

    private String targetUrl;

    @Enumerated(EnumType.STRING)
    private BannerPosition position;

    private Integer sortOrder;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Boolean isActive;
}