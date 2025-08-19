package com.honeyrest.honeyrest_user.controller.accommodation;

import com.honeyrest.honeyrest_user.dto.accommodation.AccommodationCategoryDTO;
import com.honeyrest.honeyrest_user.service.accommodation.AccommodationCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accommodations")
public class AccommodationCategoryController {

    private final AccommodationCategoryService categoryService;

    @GetMapping("/categories")
    public ResponseEntity<List<AccommodationCategoryDTO>> getAllCategories() {
        log.info("카테고리 리스트 조회 요청");
        List<AccommodationCategoryDTO> result = categoryService.getAllCategories();
        return ResponseEntity.ok(result);
    }
}