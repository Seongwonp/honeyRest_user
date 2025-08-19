package com.honeyrest.honeyrest_user.controller.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationTagMapDTO;
import com.honeyrest.honeyrest_user.service.accommodation.AccommodationTagMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accommodations")
public class AccommodationTagMapController {

    private final AccommodationTagMapService tagMapService;

    @GetMapping("/{id}/tags")
    public ResponseEntity<List<AccommodationTagMapDTO>> getTags(@PathVariable Long id) {
        log.info("숙소 태그 조회 요청: {}", id);
        return ResponseEntity.ok(tagMapService.getTagsByAccommodation(id));
    }

    @PostMapping("/{id}/tags/{tagId}")
    public ResponseEntity<Void> addTag(@PathVariable Long id, @PathVariable Long tagId) {
        log.info("숙소 태그 추가 요청: 숙소 {}, 태그 {}", id, tagId);
        tagMapService.addTagToAccommodation(id, tagId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/tags")
    public ResponseEntity<Void> removeTags(@PathVariable Long id) {
        log.info("숙소 태그 전체 삭제 요청: {}", id);
        tagMapService.removeTagsFromAccommodation(id);
        return ResponseEntity.ok().build();
    }
}