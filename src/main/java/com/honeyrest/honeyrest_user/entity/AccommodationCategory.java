package com.honeyrest.honeyrest_user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "accommodation_category")
public class AccommodationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId; // 카테고리 고유 식별자

    @Column(name = "name", nullable=false, length=100)
    private String name; // 카테고리명

    @Column(name = "icon_url", length=255)
    private String iconUrl; // 카테고리 아이콘 이미지 경로

    @Column(name = "sort_order")
    private Integer sortOrder; // 정렬 순서
}
