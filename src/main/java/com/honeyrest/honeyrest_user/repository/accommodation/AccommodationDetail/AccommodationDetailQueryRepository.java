package com.honeyrest.honeyrest_user.repository.accommodation.AccommodationDetail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.dto.CancellationPolicyDTO;
import com.honeyrest.honeyrest_user.dto.accommodation.*;
import com.honeyrest.honeyrest_user.dto.company.CompanyDTO;
import com.honeyrest.honeyrest_user.dto.location.LocationDTO;
import com.honeyrest.honeyrest_user.dto.region.RegionDTO;
import com.honeyrest.honeyrest_user.dto.review.ReviewDTO;
import com.honeyrest.honeyrest_user.dto.room.RoomDTO;
import com.honeyrest.honeyrest_user.entity.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.honeyrest.honeyrest_user.entity.QReservation.reservation;

@Log4j2
@Repository
@RequiredArgsConstructor
public class AccommodationDetailQueryRepository {

    private final JPAQueryFactory queryFactory;

    public AccommodationDetailDTO fetchDetailById(Long id, Long userId, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        QAccommodation accommodation = QAccommodation.accommodation;
        QAccommodationImage image = QAccommodationImage.accommodationImage;
        QAccommodationTagMap tagMap = QAccommodationTagMap.accommodationTagMap;
        QAccommodationTag tag = QAccommodationTag.accommodationTag;
        QRoom room = QRoom.room;
        QRoomImage roomImage = QRoomImage.roomImage;
        QReview review = QReview.review;
        QReviewImage reviewImage = QReviewImage.reviewImage;
        QCompany company = QCompany.company;
        QRegion mainRegion = new QRegion("mainRegion");
        QRegion subRegion = new QRegion("subRegion");
        QWishList wishList = QWishList.wishList;
        QCancellationPolicy qCancellationPolicy = QCancellationPolicy.cancellationPolicy;

        AccommodationDetailFlatDTO flat = queryFactory
                .select(Projections.bean(AccommodationDetailFlatDTO.class,
                        accommodation.accommodationId.as("id"),
                        accommodation.name,
                        accommodation.category.name.as("category"),
                        accommodation.address,
                        accommodation.minPrice.as("price"),
                        accommodation.description.as("intro"),
                        accommodation.amenities,
                        accommodation.checkInTime,
                        accommodation.checkOutTime,
                        accommodation.rating,
                        accommodation.latitude,
                        accommodation.longitude,
                        company.name.as("companyName"),
                        company.businessNumber.as("companyBusinessNumber"),
                        company.companyId.as("companyId"),
                        company.ownerName.as("ownerName"),
                        company.phone.as("phone"),
                        company.email.as("email"),
                        company.address.as("companyAddress"),
                        mainRegion.regionId.as("mainRegionId"),
                        mainRegion.name.as("mainRegionName"),
                        mainRegion.level.as("mainRegionLevel"),
                        mainRegion.parentId.as("mainRegionParentId"),
                        mainRegion.isPopular.as("mainRegionIsPopular"),
                        subRegion.regionId.as("subRegionId"),
                        subRegion.name.as("subRegionName")
                ))
                .from(accommodation)
                .leftJoin(accommodation.company, company)
                .leftJoin(accommodation.mainRegion, mainRegion)
                .leftJoin(accommodation.subRegion, subRegion)
                .where(accommodation.accommodationId.eq(id))
                .fetchOne();

        if (flat == null) return null;

        AccommodationDetailDTO dto = AccommodationDetailDTO.builder()
                .id(flat.getId())
                .name(flat.getName())
                .category(flat.getCategory())
                .address(flat.getAddress())
                .price(flat.getPrice())
                .intro(flat.getIntro())
                .rating(flat.getRating())
                .usage("체크인 " + formatTime(flat.getCheckInTime()) + " / 체크아웃 " + formatTime(flat.getCheckOutTime()))
                .facilities(parseJsonList(flat.getAmenities()))
                .company(CompanyDTO.builder()
                        .companyId(flat.getCompanyId())
                        .name(flat.getCompanyName())
                        .businessNumber(flat.getCompanyBusinessNumber())
                        .ownerName(flat.getOwnerName())
                        .phone(flat.getPhone())
                        .email(flat.getEmail())
                        .address(flat.getAddress())
                        .build())
                .mainRegion(RegionDTO.builder()
                        .regionId(flat.getMainRegionId())
                        .name(flat.getMainRegionName())
                        .level(flat.getMainRegionLevel())
                        .parentId(flat.getMainRegionParentId())
                        .isPopular(flat.getMainRegionIsPopular())
                        .build())
                .subRegion(RegionDTO.builder()
                        .regionId(flat.getSubRegionId())
                        .name(flat.getSubRegionName())
                        .build())
                .location(LocationDTO.builder()
                        .latitude(flat.getLatitude())
                        .longitude(flat.getLongitude())
                        .build())
                .build();

        List<String> images = queryFactory
                .select(image.imageUrl)
                .from(image)
                .where(image.accommodation.accommodationId.eq(id))
                .orderBy(image.sortOrder.asc())
                .fetch();
        dto.setImages(images);

        List<AccommodationTagDTO> tags = queryFactory
                .select(Projections.bean(AccommodationTagDTO.class,
                        tag.tagId,
                        tag.name,
                        tag.category,
                        tag.iconName
                ))
                .from(tagMap)
                .join(tagMap.tag, tag)
                .where(tagMap.accommodation.accommodationId.eq(id))
                .fetch();
        dto.setTags(tags);

        Map<Long, Long> reservedRoomCounts = queryFactory
                .select(room.roomId, reservation.reservationId.count())
                .from(reservation)
                .join(reservation.room, room)
                .where(
                        reservation.checkInDate.lt(checkOut),
                        reservation.checkOutDate.gt(checkIn),
                        reservation.status.eq("CONFIRMED"),
                        room.accommodation.accommodationId.eq(id)
                )
                .groupBy(room.roomId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(room.roomId),
                        tuple -> Optional.ofNullable(tuple.get(reservation.reservationId.count())).orElse(0L)
                ));

        BooleanBuilder roomCondition = new BooleanBuilder();
        roomCondition.and(room.accommodation.accommodationId.eq(id));
        roomCondition.and(room.status.eq("ACTIVE"));
        if (guests != null) {
            roomCondition.and(room.maxOccupancy.goe(guests));
        }

        List<RoomDTO> rooms = queryFactory
                .select(Projections.bean(RoomDTO.class,
                        room.roomId,
                        room.name,
                        room.type,
                        room.price,
                        room.maxOccupancy,
                        room.standardOccupancy,
                        room.extraPersonFee,
                        room.bedInfo.as("bedInfoJson"),
                        room.amenities.as("amenitiesJson"),
                        room.description,
                        room.totalRooms,
                        room.status
                ))
                .from(room)
                .where(roomCondition)
                .fetch();

        rooms.forEach(r -> {
            Long reserved = reservedRoomCounts.getOrDefault(r.getRoomId(), 0L);
            r.setAvailable(r.getTotalRooms() > reserved);
            r.setBedInfo(parseJsonList(r.getBedInfoJson()));
            r.setAmenities(parseJsonList(r.getAmenitiesJson()));
        });
        dto.setRooms(rooms);

        Map<Long, List<String>> roomImageMap = queryFactory
                .select(roomImage.room.roomId, roomImage.imageUrl)
                .from(roomImage)
                .where(roomImage.room.roomId.in(rooms.stream().map(RoomDTO::getRoomId).toList()))
                .orderBy(roomImage.sortOrder.asc())
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(roomImage.room.roomId),
                        Collectors.mapping(tuple -> tuple.get(roomImage.imageUrl), Collectors.toList())
                ));
        rooms.forEach(r -> r.setImages(roomImageMap.getOrDefault(r.getRoomId(), List.of())));

        List<ReviewDTO> reviews = queryFactory
                .select(Projections.bean(ReviewDTO.class,
                        review.reviewId,
                        review.rating,
                        review.content,
                        review.reply,
                        review.likeCount
                ))
                .from(review)
                .where(review.accommodationId.eq(id)
                        .and(review.status.eq("PUBLISHED")))
                .orderBy(review.reviewId.desc())
                .limit(10)
                .fetch();
        dto.setReviews(reviews);

        int reviewCount = queryFactory
                .select(review.count())
                .from(review)
                .where(review.accommodationId.eq(id)
                        .and(review.status.eq("PUBLISHED")))
                .fetchOne()
                .intValue();
        dto.setReviewCount(reviewCount);

        Map<Long, List<String>> reviewImageMap = queryFactory
                .select(reviewImage.review.reviewId, reviewImage.imageUrl)
                .from(reviewImage)
                .where(reviewImage.review.reviewId.in(reviews.stream().map(ReviewDTO::getReviewId).toList()))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(reviewImage.review.reviewId),
                        Collectors.mapping(tuple -> tuple.get(reviewImage.imageUrl), Collectors.toList())
                ));
        reviews.forEach(r -> r.setImages(reviewImageMap.getOrDefault(r.getReviewId(), List.of())));

        boolean wished = false;
        if (userId != null) {
            wished = queryFactory
                    .select(wishList.wishlistId)
                    .from(wishList)
                    .where(
                            wishList.user.userId.eq(userId),
                            wishList.accommodation.accommodationId.eq(id)
                    )
                    .fetchFirst() != null;
        }
        dto.setWished(wished);

        List<CancellationPolicyDTO> cancellationPolicies = queryFactory
                .select(Projections.bean(CancellationPolicyDTO.class,
                        qCancellationPolicy.policyId,
                        qCancellationPolicy.policyName,
                        qCancellationPolicy.detail,
                        qCancellationPolicy.accommodation.accommodationId))
                .from(qCancellationPolicy)
                .where(qCancellationPolicy.accommodation.accommodationId.eq(id))
                .fetch();

        dto.setCancellationPolicies(cancellationPolicies);

        long totalReserved = reservedRoomCounts.values().stream().mapToLong(Long::longValue).sum();
        log.info("✅ 총 예약된 Room 수: {}", totalReserved);

        reservedRoomCounts.forEach((roomId, count) ->
                log.info("🔒 Room ID {} → 예약 수량: {}", roomId, count)
        );

        boolean hasAvailableRoom = rooms.stream().anyMatch(RoomDTO::isAvailable);
        log.info("🏨 숙소 전체 예약 가능 여부: {}", hasAvailableRoom ? "가능" : "불가");

        log.info("📦 디테일 출력: {}", dto);

        return dto;
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.toLocalTime().toString() : "-";
    }

    private List<String> parseJsonList(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}