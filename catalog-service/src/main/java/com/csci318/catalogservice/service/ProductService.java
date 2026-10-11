package com.csci318.catalogservice.service;

import com.csci318.catalogservice.domain.Product;
import com.csci318.catalogservice.domain.Specification;
import com.csci318.catalogservice.domain.event.ProductCreated;
import com.csci318.catalogservice.domain.event.ProductRemoved;
import com.csci318.catalogservice.domain.event.ProductUpdated;
import com.csci318.catalogservice.infrastructure.CategoryRepository;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryClient inventoryClient;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          InventoryClient inventoryClient, ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryClient = inventoryClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String categoryId, String brand, String specName, String specValue) {
        if ((specName == null) != (specValue == null)) {
            throw new ValidationFailedException("specName and specValue must be used together");
        }
        return productRepository.search(categoryId, brand, specName, specValue);
    }

    @Transactional(readOnly = true)
    public ProductDetails getProductDetails(String productId) {
        Product product = findProduct(productId);
        return new ProductDetails(product, inventoryClient.getStockStatus(productId));
    }

    public Product createProduct(String name, Float price, String brand, String categoryId,
                                 List<Specification> specifications) {
        requireCategoryExists(categoryId);
        Product product = productRepository.save(new Product(name, price, brand, categoryId, specifications));
        eventPublisher.publishEvent(ProductCreated.of(product));
        return product;
    }

    public Product updateProduct(String productId, String name, Float price, String brand, String categoryId,
                                 List<Specification> specifications) {
        Product product = findProduct(productId);
        requireCategoryExists(categoryId);
        product.update(name, price, brand, categoryId, specifications);
        productRepository.save(product);
        eventPublisher.publishEvent(ProductUpdated.of(product));
        return product;
    }

    public void removeProduct(String productId) {
        Product product = findProduct(productId);
        productRepository.delete(product);
        eventPublisher.publishEvent(ProductRemoved.of(productId));
    }

    private Product findProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    private void requireCategoryExists(String categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ValidationFailedException("Category does not exist: " + categoryId);
        }
    }
}
