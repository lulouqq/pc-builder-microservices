package com.csci318.catalogservice.presentation.dto;

import com.csci318.catalogservice.domain.Category;

public record CategoryResponse(String id, String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
