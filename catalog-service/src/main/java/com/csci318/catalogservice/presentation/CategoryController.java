package com.csci318.catalogservice.presentation;

import com.csci318.catalogservice.presentation.dto.CategoryCreateRequest;
import com.csci318.catalogservice.presentation.dto.CategoryResponse;
import com.csci318.catalogservice.presentation.dto.CategoryUpdateRequest;
import com.csci318.catalogservice.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> listCategories() {
        return categoryService.listCategories().stream().map(CategoryResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return CategoryResponse.from(categoryService.createCategory(request.name()));
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse updateCategory(@PathVariable String categoryId,
                                           @Valid @RequestBody CategoryUpdateRequest request) {
        return CategoryResponse.from(categoryService.updateCategory(categoryId, request.name()));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCategory(@PathVariable String categoryId) {
        categoryService.removeCategory(categoryId);
    }
}
