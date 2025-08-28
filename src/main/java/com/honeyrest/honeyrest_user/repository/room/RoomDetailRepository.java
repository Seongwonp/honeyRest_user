package com.honeyrest.honeyrest_user.repository.room;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeyrest.honeyrest_user.dto.review.ReviewDTO;
import com.honeyrest.honeyrest_user.dto.room.RoomDetailDTO;
import com.honeyrest.honeyrest_user.entity.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Repository
@RequiredArgsConstructor
public class RoomDetailRepository implements RoomDetailQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final ObjectMapper objectMapper;

    // Q 클래스 선언
    private final QRoom room = QRoom.room;
    private final QAccommodation acc = QAccommodation.accommodation;
    private final QRoomImage roomImage = QRoomImage.roomImage;
    private final QReservation reservation = QReservation.reservation;
    private final QReview review = QReview.review;
    private final QReviewImage reviewImage = QReviewImage.reviewImage;
    private final QUser user = QUser.user;

    @Override
    public RoomDetailDTO fetchRoomDetailById(Long roomId, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        log.info("Room 상세정보 조회 시작: roomId={}, checkIn={}, checkOut={}", roomId, checkIn, checkOut);

        // 1. Room + Accommodation 조인
        Room roomEntity = queryFactory
                .selectFrom(room)
                .leftJoin(room.accommodation, acc).fetchJoin()
                .where(room.roomId.eq(roomId))
                .fetchOne();

        if (roomEntity == null) {
            log.warn("Room not found: roomId={}", roomId);
            return null;
        }

        // 2. RoomImage 조회
        List<String> imageUrls = queryFactory
                .select(roomImage.imageUrl)
                .from(roomImage)
                .where(roomImage.room.roomId.eq(roomId))
                .orderBy(roomImage.sortOrder.asc())
                .fetch();

        // 3. 예약 겹침 여부 판단 (NPE 방지)
        Long reservedCount = queryFactory
                .select(reservation.count())
                .from(reservation)
                .where(
                        reservation.room.roomId.eq(roomId),
                        reservation.status.eq("CONFIRMED"),
                        reservation.checkInDate.lt(checkOut),
                        reservation.checkOutDate.gt(checkIn)
                )
                .fetchOne();

        boolean isAvailable = roomEntity.getTotalRooms() > (reservedCount != null ? reservedCount : 0);

        if (guests != null) {
            isAvailable = isAvailable && roomEntity.getMaxOccupancy() >= guests;
        }
        log.info("🔒 예약 수량: {} / 총 객실 수: {} → 예약 가능 여부: {}", reservedCount, roomEntity.getTotalRooms(), isAvailable ? "가능" : "불가");
        log.info("👥 요청 인원: {} / 최대 수용 인원: {} → 인원 조건 만족 여부: {}", guests, roomEntity.getMaxOccupancy(), (guests == null || roomEntity.getMaxOccupancy() >= guests));

        // 4. 리뷰 리스트 조회
        List<Review> reviewEntities = queryFactory
                .selectFrom(review)
                .leftJoin(review.user, user).fetchJoin()
                .where(
                        review.roomId.eq(roomId),
                        review.status.eq("PUBLISHED")
                )
                .orderBy(review.createdAt.desc())
                .limit(5)
                .fetch();

        // 5. 리뷰 이미지 매핑 + DTO 조립
        List<ReviewDTO> reviews = new ArrayList<>();
        for (Review r : reviewEntities) {
            List<String> reviewImages = queryFactory
                    .select(reviewImage.imageUrl)
                    .from(reviewImage)
                    .where(reviewImage.review.reviewId.eq(r.getReviewId()))
                    .fetch();

            reviews.add(ReviewDTO.builder()
                    .reviewId(r.getReviewId())
                    .nickname(r.getUser().getName())
                    .content(r.getContent())
                    .rating(r.getRating())
                    .createdAt(r.getCreatedAt())
                    .images(reviewImages)
                    .build());
        }

        // 6. 리뷰 총 개수 조회
        Long reviewCount = queryFactory
                .select(review.count())
                .from(review)
                .where(
                        review.roomId.eq(roomId),
                        review.status.eq("PUBLISHED")
                )
                .fetchOne();

        // 7. JSON 필드 파싱
        List<String> bedInfoList = new ArrayList<>();
        List<String> amenitiesList = new ArrayList<>();

        try {
            bedInfoList = objectMapper.readValue(roomEntity.getBedInfo(), new TypeReference<List<String>>() {});
            amenitiesList = objectMapper.readValue(roomEntity.getAmenities(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("JSON 파싱 오류: {}", e.getMessage());
        }

        // 8. DTO 조립
        RoomDetailDTO dto = RoomDetailDTO.builder()
                .roomId(roomEntity.getRoomId())
                .name(roomEntity.getName())
                .type(roomEntity.getType())
                .price(roomEntity.getPrice())
                .maxOccupancy(roomEntity.getMaxOccupancy())
                .standardOccupancy(roomEntity.getStandardOccupancy())
                .extraPersonFee(roomEntity.getExtraPersonFee())
                .description(roomEntity.getDescription())
                .available(isAvailable)
                .images(imageUrls)
                .bedInfo(bedInfoList)
                .amenities(amenitiesList)
                .accommodationName(roomEntity.getAccommodation().getName())
                .accommodationAddress(roomEntity.getAccommodation().getAddress())
                .accommodationThumbnail(roomEntity.getAccommodation().getThumbnail())
                .checkInTime(roomEntity.getAccommodation().getCheckInTime())
                .checkOutTime(roomEntity.getAccommodation().getCheckOutTime())
                .reviews(reviews)
                .reviewCount(reviewCount)
                .build();

        // 9. 최종 DTO 출력
        log.info("RoomDetailDTO 조회 완료: {}", dto);
        return dto;
    }
}