package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.exception.NotFoundException;
import com.honeyrest.honeyrest_user.dto.inquiry.*;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.domain.entity.Inquiry;
import com.honeyrest.domain.entity.User;
import com.honeyrest.honeyrest_user.repository.InquiryRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationRepository;
import com.honeyrest.honeyrest_user.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final AccommodationRepository accommodationRepository;

    // 생성
    public InquiryResponseDTO createInquiry(InquiryRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        var accommodation = accommodationRepository.findById(dto.getAccommodationId())
                .orElseThrow(() -> new NotFoundException("숙소를 찾을 수 없습니다."));

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .accommodation(accommodation)
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .isReplied(false)
                .build();
        Inquiry saved = inquiryRepository.save(inquiry);

        return InquiryResponseDTO.from(saved);
    }

    // 리스트 조회 (userId 기준) - 페이징 처리
    public PageResponseDTO<InquiryListDTO> getUserInquiries(Long userId, Pageable pageable) {
        Page<Inquiry> page = inquiryRepository.findAllByUserUserId(userId, pageable);
        List<InquiryListDTO> content = page.getContent().stream()
                .map(InquiryListDTO::from)
                .collect(Collectors.toList());
        return PageResponseDTO.<InquiryListDTO>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    // 단일 문의 조회
    @Transactional(readOnly = true)
    public InquiryDetailResponseDTO getInquiryById(Long userId, Long inquiryId) {
        return InquiryDetailResponseDTO.from(findOwnedInquiry(inquiryId, userId));
    }

    // 삭제
    @Transactional
    public void deleteInquiry(Long inquiryId, Long userId) {
        inquiryRepository.delete(findOwnedInquiry(inquiryId, userId));
    }

    /**
     * 수정. 관리 중인 엔티티를 도메인 메서드로 바꿔 답변(reply/isReplied)·카테고리·숙소를 보존한다.
     * (과거에는 빌더로 새 엔티티를 만들어 save 해 reply·category 가 NULL 로 지워졌다.)
     * 답변이 달린 문의는 수정할 수 없다 → IllegalStateException(409).
     */
    @Transactional
    public void updateInquiry(Long inquiryId, Long userId, InquiryRequestDTO dto) {
        Inquiry inquiry = findOwnedInquiry(inquiryId, userId);
        inquiry.editQuestion(dto.getTitle(), dto.getContent(), dto.getCategory());
    }

    /** 문의가 없으면 404, 다른 사용자의 문의면 403. (과거에는 RuntimeException 으로 500 이 났다.) */
    private Inquiry findOwnedInquiry(Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NotFoundException("문의 내역을 찾을 수 없습니다."));
        if (userId == null || inquiry.getUser() == null || !userId.equals(inquiry.getUser().getUserId())) {
            throw new ApiException("본인의 문의만 조회·수정·삭제할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        return inquiry;
    }

    public InquiryCountDTO getUserInquiryCounts(Long userId) {
        long total = inquiryRepository.countByUserUserId(userId);
        long answered = inquiryRepository.countByUserUserIdAndIsRepliedTrue(userId);
        long unanswered = total - answered;

        return InquiryCountDTO.builder()
                .totalCount(total)
                .answeredCount(answered)
                .unansweredCount(unanswered)
                .build();
    }



}
