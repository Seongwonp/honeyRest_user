package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.inquiry.*;
import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.entity.Inquiry;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.InquiryRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.accommodation.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        var accommodation = accommodationRepository.findById(dto.getAccommodationId())
                .orElseThrow(() -> new RuntimeException("숙소를 찾을 수 없습니다."));

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .accommodation(accommodation)
                .title(dto.getTitle())
                .content(dto.getContent())
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
    public InquiryDetailResponseDTO getInquiryById(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의 내역을 찾을 수 없습니다."));

        if (!inquiry.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        return InquiryDetailResponseDTO.from(inquiry);
    }

    // 삭제
    public void deleteInquiry(Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의 내역을 찾을 수 없습니다."));
        if (!inquiry.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }
        inquiryRepository.delete(inquiry);
    }

    // 수정
    public void updateInquiry(Long inquiryId, Long userId, InquiryRequestDTO dto) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의 내역을 찾을 수 없습니다."));

        if (!inquiry.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        Inquiry updatedInquiry = Inquiry.builder()
                .inquiryId(inquiry.getInquiryId())
                .user(inquiry.getUser())
                .accommodation(inquiry.getAccommodation())
                .title(dto.getTitle())
                .content(dto.getContent())
                .isReplied(inquiry.getIsReplied())
                .build();

        Inquiry saved = inquiryRepository.save(updatedInquiry);
        InquiryResponseDTO.from(saved);
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
