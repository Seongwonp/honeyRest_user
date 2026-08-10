package com.honeyrest.honeyrest_user.controller.user;

import com.honeyrest.honeyrest_user.dto.WishlistedAccommodationDTO;
import com.honeyrest.honeyrest_user.dto.coupon.UserCouponDTO;
import com.honeyrest.honeyrest_user.dto.inquiry.*;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.dto.point.PointHistoryDTO;
import com.honeyrest.honeyrest_user.dto.point.PointHistoryResponseDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationDetailDTO;
import com.honeyrest.honeyrest_user.dto.reservation.ReservationSummaryDTO;
import com.honeyrest.honeyrest_user.dto.review.MyReviewDTO;
import com.honeyrest.honeyrest_user.dto.review.ReviewRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.EmailChangeRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.PasswordChangeRequestDTO;
import com.honeyrest.honeyrest_user.dto.user.UserInfoDTO;
import com.honeyrest.honeyrest_user.dto.user.UserProfileUpdateRequestDTO;
import com.honeyrest.honeyrest_user.security.CustomUserPrincipal;
import com.honeyrest.honeyrest_user.service.*;
import com.honeyrest.honeyrest_user.service.coupon.UserCouponService;
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
    private final UserCouponService userCouponService;
    private final InquiryService inquiryService;
    private final PointHistoryService pointHistoryService;


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
        log.info("프로필 업데이트: userId={}", principal.getUserId());
        userService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/point-history")
    public ResponseEntity<PointHistoryResponseDTO> getPointHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = DESC) Pageable pageable
    ){
        PointHistoryResponseDTO response = pointHistoryService.getHistoryByUser(principal.getUserId(), pageable);
        log.info("포인트 사용내역 요청: userId={}", principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-email-change")
    public ResponseEntity<?> requestEmailChange(
            @RequestBody EmailChangeRequestDTO dto,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        log.info("계정 이메일 변경 요청: userId={}", principal.getUserId());
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
        log.info("🔐 비밀번호 변경 요청: userId={}", userId);

        userService.changePassword(userId, dto.getCurrentPassword(), dto.getNewPassword());

        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }


    @DeleteMapping("/delete-account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = principal.getUserId();
        userService.deleteAccount(userId);

        log.info("🚫 회원 탈퇴 요청 처리 완료: userId={}", userId);

        return ResponseEntity.noContent().build();
    }


    /* 문의 생성 */
    @PostMapping("/inquiries")
    public ResponseEntity<InquiryResponseDTO> createInquiry(
            @RequestBody InquiryRequestDTO request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build(); // 비회원 접근 차단
        }

        Long userId = principal.getUserId();
        request.setUserId(userId);

        // 제목/내용이 없으면 기본값 설정
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            request.setTitle("제목 없음");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            request.setContent("내용 없음");
        }

        InquiryResponseDTO dto = inquiryService.createInquiry(request);
        log.info("✅ 문의 생성: userId={}, inquiryId={}", userId, dto.getInquiryId());
        return ResponseEntity.ok(dto);
    }

    /* 문의 리스트 조회 (페이징) */
    @GetMapping("/inquiries/List")
    public ResponseEntity<PageResponseDTO<InquiryListDTO>> getMyInquiries(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 5, sort = "createdAt", direction = DESC) Pageable pageable
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build(); // 비회원 접근 차단
        }

        Long userId = principal.getUserId();
        return ResponseEntity.ok(inquiryService.getUserInquiries(userId, pageable));
    }

    @GetMapping("/inquiries/counts")
    public ResponseEntity<InquiryCountDTO> getMyInquiryCounts(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = principal.getUserId();
        InquiryCountDTO counts = inquiryService.getUserInquiryCounts(userId);
        return ResponseEntity.ok(counts);
    }


    /* 단일 문의 조회 */
    @GetMapping("/inquiries/{inquiryId}")
    public ResponseEntity<InquiryDetailResponseDTO> getInquiry(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build(); // 비회원 접근 차단
        }

        Long userId = principal.getUserId();
        InquiryDetailResponseDTO dto = inquiryService.getInquiryById(userId, inquiryId);
        log.info("🔍 문의 조회: userId={}, inquiryId={}", userId, inquiryId);
        return ResponseEntity.ok(dto);
    }

    /* 문의 수정 */
    @PutMapping("/inquiries/edit/{inquiryId}")
    public ResponseEntity<Void> updateInquiry(
            @PathVariable Long inquiryId,
            @RequestBody InquiryRequestDTO request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build(); // 비회원 접근 차단
        }

        Long userId = principal.getUserId();

        // 제목/내용이 없으면 기본값 설정
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            request.setTitle("제목 없음");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            request.setContent("내용 없음");
        }

        inquiryService.updateInquiry(userId, inquiryId, request);
        log.info("✏️ 문의 수정: userId={}, inquiryId={}", userId, inquiryId);
        return ResponseEntity.ok().build();
    }

    /* 문의 삭제 */
    @DeleteMapping("/inquiries/delete/{inquiryId}")
    public ResponseEntity<Void> deleteInquiry(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build(); // 비회원 접근 차단
        }

        Long userId = principal.getUserId();
        inquiryService.deleteInquiry(userId, inquiryId);
        log.info("🗑️ 문의 삭제: userId={}, inquiryId={}", userId, inquiryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/coupons")
    public ResponseEntity<PageResponseDTO<UserCouponDTO>> getMyCoupons(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 5, sort = "issuedAt", direction = DESC) Pageable pageable
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build(); // 비회원 접근 차단
        }

        Long userId = principal.getUserId();
        PageResponseDTO<UserCouponDTO> dto = userCouponService.getUserCoupons(userId, pageable);
        log.info("쿠폰 정보 조회: userId={}", userId);
        return ResponseEntity.ok(dto);
    }

}
