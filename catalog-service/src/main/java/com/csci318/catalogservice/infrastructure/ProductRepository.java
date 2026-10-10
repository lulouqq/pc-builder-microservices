package com.csci318.catalogservice.infrastructure;

import com.csci318.catalogservice.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    boolean existsByCategoryId(String categoryId);

    /**
     * Browse and search products (C1). Every filter is optional (null = not applied) and the filters
     * are combined with AND. Brand and specification matching ignores case.
     */
    @Query("""
            select p from Product p
            where (:categoryId is null or p.categoryId = :categoryId)
              and (:brand is null or lower(p.brand) = lower(:brand))
              and (:specName is null or exists (
                    select 1 from Product p2 join p2.specifications s
                    where p2 = p and lower(s.name) = lower(:specName) and lower(s.value) = lower(:specValue)))
            order by p.name
            """)
    List<Product> search(@Param("categoryId") String categoryId, @Param("brand") String brand,
                         @Param("specName") String specName, @Param("specValue") String specValue);
}
