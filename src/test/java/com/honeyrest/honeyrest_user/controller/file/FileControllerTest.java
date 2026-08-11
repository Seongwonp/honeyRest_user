package com.honeyrest.honeyrest_user.controller.file;

import com.honeyrest.honeyrest_user.entity.Review;
import com.honeyrest.honeyrest_user.entity.ReviewImage;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.review.ReviewImageRepository;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.util.FileUploadUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * P1-5 회귀 테스트: 이미지 삭제는 화이트리스트에 없는 folder나 본인 소유가 아닌 imgUrl을
 * 거부해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock private FileUploadUtil fileUploadUtil;
    @Mock private UserRepository userRepository;
    @Mock private ReviewImageRepository reviewImageRepository;

    private FileController controller;
    private CustomUserPrincipal me;

    @BeforeEach
    void setUp() {
        controller = new FileController(fileUploadUtil, userRepository, reviewImageRepository);
        User user = User.builder().userId(1L).build();
        me = new CustomUserPrincipal(user, Map.of());
    }

    @Test
    void 허용되지_않은_폴더는_삭제가_거부된다() {
        assertThatThrownBy(() -> controller.deleteFile(me, "banner", "https://example.com/banner.png"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(fileUploadUtil);
    }

    @Test
    void 타인의_프로필_이미지는_삭제가_거부된다() {
        User owner = User.builder().userId(1L).profileImage("https://example.com/my-profile.png").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> controller.deleteFile(me, "profile", "https://example.com/someone-else-profile.png"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(fileUploadUtil);
    }

    @Test
    void 본인_프로필_이미지는_삭제된다() {
        User owner = User.builder().userId(1L).profileImage("https://example.com/my-profile.png").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        controller.deleteFile(me, "profile", "https://example.com/my-profile.png");

        verify(fileUploadUtil).delete("profile", "https://example.com/my-profile.png");
    }

    @Test
    void 타인의_리뷰_이미지는_삭제가_거부된다() {
        User otherUser = User.builder().userId(99L).build();
        Review othersReview = Review.builder().reviewId(10L).user(otherUser).build();
        ReviewImage image = ReviewImage.builder().imageId(1L).review(othersReview)
                .imageUrl("https://example.com/review.png").build();
        when(reviewImageRepository.findByImageUrl("https://example.com/review.png"))
                .thenReturn(Optional.of(image));

        assertThatThrownBy(() -> controller.deleteFile(me, "reviews", "https://example.com/review.png"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(fileUploadUtil);
    }
}
