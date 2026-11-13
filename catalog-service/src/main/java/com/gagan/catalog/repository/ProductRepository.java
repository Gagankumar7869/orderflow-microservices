package com.gagan.catalog.repository;

import com.gagan.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.isActive = true")
    List<Product> findAllActiveWithCategory();

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.categoryId = :categoryId AND p.isActive = true")
    List<Product> findActiveByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.productId = :productId")
    Optional<Product> findByIdWithCategory(@Param("productId") Long productId);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE UPPER(p.sku) = UPPER(:sku)")
    Optional<Product> findBySkuWithCategory(@Param("sku") String sku);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
           "WHERE p.productId = :productId AND p.isActive = true AND p.stockQuantity >= :quantity")
    int deductStockAtomically(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :quantity " +
           "WHERE p.productId = :productId AND p.isActive = true")
    int restoreStockAtomically(@Param("productId") Long productId, @Param("quantity") int quantity);
}