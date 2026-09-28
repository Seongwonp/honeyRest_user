package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.WishlistedAccommodationDTO;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.domain.entity.Accommodation;
import com.honeyrest.domain.entity.User;
import com.honeyrest.domain.entity.WishList;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.wishList.WishListQueryRepository;
import com.honeyrest.honeyrest_user.repository.wishList.WishListRepository;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationRepository;
import com.honeyrest.honeyrest_user.service.redis.RatingCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
@RequiredArgsConstructor
public class WishListService {

    private final WishListRepository wishListRepository;
    private final WishListQueryRepository  wishListQueryRepository;
    private final UserRepository userRepository;
    private final AccommodationRepository accommodationRepository;
    private final RatingCacheService ratingCacheService;
    private final RedisTemplate<String, Object> redisTemplate;

    public boolean toggleWish(Long userId, Long accommodationId) {
        log.info("🧡 찜 토글 실행: userId={}, accommodationId={}", userId, accommodationId);

        Optional<WishList> existing = wishListRepository.findByUser_UserIdAndAccommodation_AccommodationId(userId, accommodationId);

        boolean result;
        if (existing.isPresent()) {
            wishListRepository.delete(existing.get());
            log.info("💔 찜 삭제 완료");
            result = false;
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
            Accommodation accommodation = accommodationRepository.findById(accommodationId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 숙소입니다."));

            WishList wish = WishList.builder()
                    .user(user)
                    .accommodation(accommodation)
                    .build();

            wishListRepository.save(wish);
            log.info("💖 찜 추가 완료");
            result = true;
        }


        // 찜 토글 후 숙소 관련 모든 캐시 삭제
        ratingCacheService.evictAllAccommodationCache(accommodationId);
        log.info("🗑️ 숙소 관련 모든 캐시 삭제 완료: accommodationId={}", accommodationId);

        return result;
    }

    public PageResponseDTO<WishlistedAccommodationDTO> getWishlistedAccommodations(Long userId, Pageable pageable) {
        Page<WishlistedAccommodationDTO> page = wishListQueryRepository.findWishlistedAccommodations(userId, pageable);

        return PageResponseDTO.<WishlistedAccommodationDTO>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }



}