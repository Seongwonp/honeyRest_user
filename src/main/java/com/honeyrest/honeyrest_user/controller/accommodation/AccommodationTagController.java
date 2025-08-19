package com.honeyrest.honeyrest_user.controller.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationTagDTO;
import com.honeyrest.honeyrest_user.service.accommodation.AccommodationTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accommodations")
public class AccommodationTagController {

    private final AccommodationTagService tagService;

    @GetMapping("/tags")
    public ResponseEntity<List<AccommodationTagDTO>> getAllTags() {
        log.info("태그 리스트 조회 요청");
        List<AccommodationTagDTO> result = tagService.getAllTags();
        return ResponseEntity.ok(result);
    }
}