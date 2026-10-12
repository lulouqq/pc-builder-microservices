package com.csci318.catalogservice.service;

import com.csci318.catalogservice.domain.Category;
import com.csci318.catalogservice.infrastructure.CategoryRepository;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> listCategories() {
        return categoryRepository.findAll(Sort.by("name"));
    }

    public Category createCategory(String name) {
        return categoryRepository.save(new Category(name));
    }

    public Category updateCategory(String categoryId, String name) {
        Category category = findCategory(categoryId);
        category.rename(name);
        return categoryRepository.save(category);
    }

    public void removeCategory(String categoryId) {
        Category category = findCategory(categoryId);
        if (productRepository.existsByCategoryId(categoryId)) {
            throw new CategoryInUseException("Category still has products: " + categoryId);
        }
        categoryRepository.delete(category);
    }

    private Category findCategory(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }
}
