package sneak_shop.controller;

import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.dto.response.CategoryResponse;
import sneak_shop.service.impl.CategoryServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryServiceImpl categoryService;

    public CategoryController(CategoryServiceImpl categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAll() {
        return ApiResponse.ok(categoryService.getAll());
    }

    @GetMapping("/roots")
    public ApiResponse<List<CategoryResponse>> getRoots() {
        return ApiResponse.ok(categoryService.getRoots());
    }

    @GetMapping("/{slug}")
    public ApiResponse<CategoryResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.ok(categoryService.getBySlug(slug));
    }
}
