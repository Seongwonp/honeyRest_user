package com.honeyrest.honeyrest_user.repository.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationSearchDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationTagMapDTO;
import com.honeyrest.honeyrest_user.entity.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AccommodationSearchImpl implements AccommodationSearch {

    private final EntityManager em;

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(em);
    }

    @Override
    public Page<AccommodationSearchDTO> searchAvailable(
            String location,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            Long userId,
            String sort,
            List<String> selectedCategories,
            List<String> selectedTags,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        QAccommodation a = QAccommodation.accommodation;
        QRoom r = QRoom.room;
        QReview rv = QReview.review;
        QWishList w = QWishList.wishList;
        QReservation res = QReservation.reservation;
        QAccommodationTagMap atm = QAccommodationTagMap.accommodationTagMap;
        QAccommodationTag at = QAccommodationTag.accommodationTag;

        BooleanBuilder builder = new BooleanBuilder();

        if (location != null && !location.isBlank()) {
            builder.and(
                    a.mainRegion.name.containsIgnoreCase(location)
                            .or(a.subRegion.name.containsIgnoreCase(location))
                            .or(a.address.containsIgnoreCase(location))
            );
        }

        builder.and(r.maxOccupancy.goe(guests));

        builder.and(
                JPAExpressions.selectOne()
                        .from(res)
                        .where(
                                res.room.roomId.eq(r.roomId),
                                res.checkInDate.lt(checkOut),
                                res.checkOutDate.gt(checkIn)
                        )
                        .exists()
                        .not()
        );

        if (maxPrice != null) {
            builder.and(a.minPrice.loe(maxPrice));
        }

        if (selectedCategories != null && !selectedCategories.isEmpty()) {
            builder.and(a.category.name.in(selectedCategories));
        }

        if (selectedTags != null && !selectedTags.isEmpty()) {
            builder.and(
                    a.accommodationId.in(
                            JPAExpressions.select(atm.accommodation.accommodationId)
                                    .from(atm)
                                    .join(at).on(atm.tag.tagId.eq(at.tagId))
                                    .where(at.name.in(selectedTags))
                                    .groupBy(atm.accommodation.accommodationId)
                                    .having(at.name.countDistinct().eq((long) selectedTags.size()))
                    )
            );
        }

        var order = switch (sort) {
            case "priceAsc" -> a.minPrice.asc();
            case "priceDesc" -> a.minPrice.desc();
            case "ratingDesc" -> a.rating.desc();
            case "latest" -> a.accommodationId.desc();
            case "random" -> Expressions.numberTemplate(Double.class, "rand()").asc();
            default -> a.minPrice.asc();
        };

        List<AccommodationSearchDTO> results = queryFactory()
                .select(Projections.constructor(AccommodationSearchDTO.class,
                        a.accommodationId,
                        a.name,
                        a.address,
                        a.thumbnail,
                        a.minPrice,
                        a.minPrice,
                        a.rating,
                        rv.reviewId.countDistinct(),
                        a.category.name,
                        w.wishlistId.isNotNull(),
                        a.mainRegion.name,
                        a.subRegion.name
                ))
                .from(a)
                .join(r).on(r.accommodation.accommodationId.eq(a.accommodationId))
                .leftJoin(w).on(
                        w.accommodation.accommodationId.eq(a.accommodationId)
                                .and(userId != null ? w.user.userId.eq(userId) : Expressions.FALSE.isTrue())
                )
                .leftJoin(rv).on(rv.accommodationId.eq(a.accommodationId))
                .where(builder)
                .groupBy(a.accommodationId)
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory()
                .select(a.accommodationId.countDistinct())
                .from(a)
                .join(r).on(r.accommodation.accommodationId.eq(a.accommodationId))
                .where(builder)
                .fetchOne();


        List<Long> accommodationIds = results.stream()
                .map(AccommodationSearchDTO::getId)
                .toList();

        List<AccommodationTagMapDTO> tagDTOs = queryFactory()
                .select(Projections.constructor(AccommodationTagMapDTO.class,
                        atm.mapId,
                        atm.accommodation.accommodationId,
                        at.tagId,
                        at.name,
                        at.category
                ))
                .from(atm)
                .join(at).on(atm.tag.tagId.eq(at.tagId))
                .where(atm.accommodation.accommodationId.in(accommodationIds))
                .fetch();

        Map<Long, List<AccommodationTagMapDTO>> tagMap = tagDTOs.stream()
                .collect(Collectors.groupingBy(AccommodationTagMapDTO::getAccommodationId));

        results.forEach(dto -> dto.setTags(tagMap.getOrDefault(dto.getId(), List.of())));

        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }
}