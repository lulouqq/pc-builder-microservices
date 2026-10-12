package com.csci318.catalogservice.infrastructure;

import com.csci318.catalogservice.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {
}
