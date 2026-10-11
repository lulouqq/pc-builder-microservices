package com.csci318.catalogservice.presentation.dto;

import com.csci318.catalogservice.domain.Specification;
import jakarta.validation.constraints.NotBlank;

public record SpecificationDto(@NotBlank String name, @NotBlank String value) {

    public static SpecificationDto from(Specification specification) {
        return new SpecificationDto(specification.getName(), specification.getValue());
    }

    public Specification toDomain() {
        return new Specification(name, value);
    }
}
