package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.point.PointHistoryResponseDTO;
import com.honeyrest.honeyrest_user.dto.point.PointHistoryDTO;
import com.honeyrest.domain.entity.PointHistory;
import com.honeyrest.honeyrest_user.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 포인트 이력 조회 전용 서비스.
 * 잔액 변경과 이력 기록은 함께 처리해야 하므로 {@link PointService} 가 담당한다.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class PointHistoryService {

    private final PointHistoryRepository pointHistoryRepository;

    @Transactional(readOnly = true)
    public PointHistoryResponseDTO getHistoryByUser(Long userId, Pageable pageable) {

        Page<PointHistory> entities = pointHistoryRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);
        List<PointHistoryDTO> dtoList = entities.stream()
                .map(h -> PointHistoryDTO.builder()
                        .amount(h.getAmount())
                        .type(h.getType())
                        .reason(h.getReason())
                        .relatedId(h.getRelatedId())
                        .balance(h.getBalance())
                        .createdAt(h.getCreatedAt())
                        .expiresAt(h.getExpiresAt())
                        .build())
                .toList();

        return PointHistoryResponseDTO.builder()
                .currentPoint(entities.getContent().isEmpty() ? 0 : entities.getContent().get(0).getUser().getPoint())
                .history(dtoList)
                .totalElements(entities.getTotalElements())
                .totalPages(entities.getTotalPages())
                .page(entities.getNumber())
                .size(entities.getSize())
                .build();
    }


}
