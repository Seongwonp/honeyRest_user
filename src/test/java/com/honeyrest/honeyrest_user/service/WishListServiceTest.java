package com.honeyrest.honeyrest_user.service;

import com.honeyrest.domain.entity.WishList;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationRepository;
import com.honeyrest.honeyrest_user.repository.wishList.WishListQueryRepository;
import com.honeyrest.honeyrest_user.repository.wishList.WishListRepository;
import com.honeyrest.honeyrest_user.service.redis.RatingCacheService;
import com.honeyrest.honeyrest_user.service.redis.SearchCacheVersionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishListService: 찜 토글은 검색 캐시 세대를 올린다")
class WishListServiceTest {

    @Mock private WishListRepository wishListRepository;
    @Mock private WishListQueryRepository wishListQueryRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccommodationRepository accommodationRepository;
    @Mock private RatingCacheService ratingCacheService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private SearchCacheVersionService searchCacheVersionService;

    @InjectMocks private WishListService wishListService;

    @Test
    void 찜_해제도_검색_결과의_찜_여부가_바뀌므로_세대를_올린다() {
        WishList existing = WishList.builder().wishlistId(1L).build();
        when(wishListRepository.findByUser_UserIdAndAccommodation_AccommodationId(1L, 2L)).thenReturn(Optional.of(existing));

        boolean result = wishListService.toggleWish(1L, 2L);

        assertThat(result).isFalse();
        verify(wishListRepository).delete(existing);
        verify(ratingCacheService).evictAllAccommodationCache(2L);
        verify(searchCacheVersionService).bumpAfterCommit();
    }
}
