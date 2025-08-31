package com.honeyrest.honeyrest_user.repository.wishList;

import com.honeyrest.honeyrest_user.dto.WishlistedAccommodationDTO;
import com.honeyrest.honeyrest_user.entity.QAccommodation;
import com.honeyrest.honeyrest_user.entity.QAccommodationCategory;
import com.honeyrest.honeyrest_user.entity.QRegion;
import com.honeyrest.honeyrest_user.entity.QWishList;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WishListQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<WishlistedAccommodationDTO> findWishlistedAccommodations(Long userId, Pageable pageable) {
        QWishList wishList = QWishList.wishList;
        QAccommodation accommodation = QAccommodation.accommodation;
        QAccommodationCategory category = QAccommodationCategory.accommodationCategory;
        QRegion mainRegion = QRegion.region;
        QRegion subRegion = new QRegion("subRegion");

        List<WishlistedAccommodationDTO> content = queryFactory
                .select(Projections.constructor(WishlistedAccommodationDTO.class,
                        accommodation.accommodationId,
                        accommodation.name,
                        accommodation.address,
                        accommodation.thumbnail,
                        accommodation.minPrice,
                        accommodation.rating,
                        category.name,
                        mainRegion.name,
                        subRegion.name
                ))
                .from(wishList)
                .join(wishList.accommodation, accommodation)
                .join(accommodation.category, category)
                .join(accommodation.mainRegion, mainRegion)
                .join(accommodation.subRegion, subRegion)
                .where(wishList.user.userId.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPQLQuery<Long> countQuery = queryFactory
                .select(wishList.count())
                .from(wishList)
                .where(wishList.user.userId.eq(userId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}