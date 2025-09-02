package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.page.PageResponseDTO;
import com.honeyrest.honeyrest_user.dto.point.PointHistoryDTO;
import com.honeyrest.honeyrest_user.dto.point.PointHistoryRequestDTO;
import com.honeyrest.honeyrest_user.entity.PointHistory;
import com.honeyrest.honeyrest_user.entity.Reservation;
import com.honeyrest.honeyrest_user.entity.User;
import com.honeyrest.honeyrest_user.repository.PointHistoryRepository;
import com.honeyrest.honeyrest_user.repository.UserRepository;
import com.honeyrest.honeyrest_user.repository.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class PointHistoryService {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;


    @Transactional
    public void addHistory(PointHistoryRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        int current = user.getPoint();

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount(dto.getAmount())
                .type(dto.getType())
                .reason(dto.getReason())
                .relatedId(dto.getRelatedId())
                .balance(current)
                .expiresAt(dto.getExpiresAt())
                .build();

        pointHistoryRepository.save(history);

        log.info("📄 포인트 내역 추가 완료: userId={}, amount={}, type={}, balance={}",
                dto.getUserId(), dto.getAmount(), dto.getType(), current);
    }



    @Transactional
    public void recordUsage(Long userId, Long reservationId, Integer usedPoint) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다"));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예약 정보를 찾을 수 없습니다"));

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount(-usedPoint)
                .type("USE")
                .reason("예약 시 포인트 사용")
                .relatedId(reservationId)
                .balance(user.getPoint()) // 이미 차감된 상태 기준
                .build();

        pointHistoryRepository.save(history);

        log.info("📄 포인트 사용 이력 저장 완료: userId={}, reservationId={}, usedPoint={}, balance={}",
                userId, reservationId, usedPoint, user.getPoint());
    }

    public PageResponseDTO<PointHistoryDTO> getHistoryByUser(Long userId, Pageable pageable) {
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

        return PageResponseDTO.<PointHistoryDTO>builder()
                .content(dtoList)
                .totalElements(entities.getTotalElements())
                .totalPages(entities.getTotalPages())
                .page(entities.getNumber())
                .size(entities.getSize())
                .build();
    }


}
