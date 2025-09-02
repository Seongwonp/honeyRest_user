package com.honeyrest.honeyrest_user.service;

import com.honeyrest.honeyrest_user.dto.CancellationPolicyDTO;
import com.honeyrest.honeyrest_user.entity.CancellationPolicy;
import com.honeyrest.honeyrest_user.repository.CancellationPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CancellationPolicyService {

    private final CancellationPolicyRepository cancellationPolicyRepository;

    public List<CancellationPolicyDTO> getCancellationPoliciesByAccommodationId(Long accommodationId) {
        List<CancellationPolicy> entities = cancellationPolicyRepository.findByAccommodation_AccommodationId(accommodationId);

        return entities.stream()
                .map(policy -> CancellationPolicyDTO.builder()
                        .accommodationId(policy.getAccommodation().getAccommodationId())
                        .policyId(policy.getPolicyId())
                        .policyName(policy.getPolicyName())
                        .detail(policy.getDetail())
                        .build())
                .toList();
    }

}
