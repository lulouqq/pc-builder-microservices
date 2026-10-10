package com.csci318.catalogservice.presentation;

import com.csci318.catalogservice.domain.Product;
import com.csci318.catalogservice.domain.Specification;
import com.csci318.catalogservice.presentation.dto.ProductCreateRequest;
import com.csci318.catalogservice.presentation.dto.ProductResponse;
import com.csci318.catalogservice.presentation.dto.ProductUpdateRequest;
import com.csci318.catalogservice.presentation.dto.SpecificationDto;
import com.csci318.catalogservice.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> searchProducts(@RequestParam(required = false) String categoryId,
                                                @RequestParam(required = false) String brand,
                                                @RequestParam(required = false) String specName,
                                                @RequestParam(required = false) String specValue) {
        return productService.searchProducts(categoryId, brand, specName, specValue).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable String productId) {
        return ProductResponse.from(productService.getProductDetails(productId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductCreateRequest request) {
        Product product = productService.createProduct(request.name(), request.price(), request.brand(),
                request.categoryId(), toDomain(request.specifications()));
        return ProductResponse.from(product);
    }

    @PutMapping("/{productId}")
    public ProductResponse updateProduct(@PathVariable String productId,
                                         @Valid @RequestBody ProductUpdateRequest request) {
        Product product = productService.updateProduct(productId, request.name(), request.price(), request.brand(),
                request.categoryId(), toDomain(request.specifications()));
        return ProductResponse.from(product);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProduct(@PathVariable String productId) {
        productService.removeProduct(productId);
    }

    private List<Specification> toDomain(List<SpecificationDto> specifications) {
        return specifications.stream().map(SpecificationDto::toDomain).toList();
    }
}
