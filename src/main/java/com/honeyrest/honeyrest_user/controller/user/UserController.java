package com.honeyrest.honeyrest_user.controller.user;

import com.honeyrest.honeyrest_user.dto.WishlistedAccommodationDTO;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationDetailDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationSummaryDTO;
import com.honeyrest.honeyrest_user.dto.review.MyReviewDTO;
import com.honeyrest.honeyrest_user.dto.review.ReviewRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.EmailChangeRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.PasswordChangeRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.UserInfoDTO;
import com.honeyrest.honeyrest_user.dto.user.UserProfileUpdateRequestDTO;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.service.ReviewService;
import com.honeyrest.honeyrest_user.service.UserService;
import com.honeyrest.honeyrest_user.service.WishListService;
import com.honeyrest.honeyrest_user.service.email.EmailVerificationTokenService;
import com.honeyrest.honeyrest_user.service.reservation.ReserveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final ReserveService reserveService;
    private final UserService userService;
    private final ReviewService reviewService;
    private final WishListService wishListService;
    private final EmailVerificationTokenService emailVerificationTokenService;


    @GetMapping("/info")
    public ResponseEntity<UserInfoDTO> getUserInfo(@AuthenticationPrincipal CustomUserPrincipal customUserPrincipal) {
        Long userId = customUserPrincipal.getUserId();
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }


    @GetMapping("/reservations")
    public ResponseEntity<PageResponseDTO<ReservationSummaryDTO>> getMyReservations(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 5, sort = "checkInDate", direction = DESC) Pageable pageable) {

        Long userId = principal.getUserId(); // 토큰에서 추출된 인증된 사용자 ID

        return ResponseEntity.ok(reserveService.getUserReservationSummary(userId, pageable));
    }


    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<ReservationDetailDTO> getReservationDetail(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long userId = principal.getUserId();
        ReservationDetailDTO dto = reserveService.getReservationDetail(userId, reservationId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/reservations/{reservationId}/cancel-request")
    public ResponseEntity<Void> requestReservationCancel(
            @PathVariable Long reservationId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        String reason = body.get("reason");
        Long userId = principal.getUserId();

        reserveService.requestReservationCancel(userId, reservationId, reason);
        log.info("✅ 예약 취소 요청 완료: userId={}, reservationId={}, reason={}", userId, reservationId, reason);

        return ResponseEntity.ok().build();
    }


    @GetMapping("/reviews")
    public ResponseEntity<PageResponseDTO<MyReviewDTO>> getMyReviews(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 5, sort = "createdAt", direction = DESC) Pageable pageable
    ) {
        Long userId = principal.getUserId();
        return ResponseEntity.ok(reviewService.getUserReviews(userId, pageable));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        reviewService.deleteReview(principal.getUserId(), reviewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/{reviewId}/update")
    public ResponseEntity<Void> updateReview(
            @PathVariable Long reviewId,
            @RequestPart("review") ReviewRequestDTO reviewDto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) throws Exception {
        reviewService.updateReviewWithImages(principal.getUserId(), reviewId, reviewDto, newImages);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/wishlist")
    public ResponseEntity<PageResponseDTO<WishlistedAccommodationDTO>> getMyWishlists(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @PageableDefault(size = 5, sort = "createdAt", direction = DESC) Pageable pageable
    ){
        Long userId = principal.getUserId();
        return ResponseEntity.ok(wishListService.getWishlistedAccommodations(userId, pageable));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody UserProfileUpdateRequestDTO request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        log.info("프로필 업데이트: {}",request);
        userService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/request-email-change")
    public ResponseEntity<?> requestEmailChange(
            @RequestBody EmailChangeRequestDTO dto,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        log.info("계정 이메일 변경: {}", dto);
        emailVerificationTokenService.sendEmailChangeToken(principal.getUserId(), dto.getNewEmail(), dto.isPasswordVerified());
        return ResponseEntity.ok("인증 메일을 발송했습니다.");
    }

    @PostMapping("/profile-image")
    public ResponseEntity<String> updateProfileImage(
            @RequestParam("profileImage") MultipartFile file,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) throws Exception {
        String imageUrl = userService.updateProfileImage(principal.getUserId(), file);
        log.info("이미지 업데이트완료: {}",imageUrl);
        return ResponseEntity.ok("이미지가 성공적으로 수정되었습니다!");
    }


    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody PasswordChangeRequestDTO dto
    ) {
        Long userId = principal.getUserId();
        log.info("🔐 비밀번호 변경 요청: userId={}, dto={}", userId, dto);

        userService.changePassword(userId, dto.getCurrentPassword(), dto.getNewPassword());

        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    @PostMapping("/point")
    public ResponseEntity<?> changePoint(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ){

        return null;
    }


}
