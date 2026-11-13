package com.gagan.catalog.service.impl;

import com.gagan.catalog.constant.ErrorCodeEnum;
import com.gagan.catalog.dto.ProductRequest;
import com.gagan.catalog.dto.ProductResponse;
import com.gagan.catalog.entity.Category;
import com.gagan.catalog.entity.Product;
import com.gagan.catalog.exception.CatalogServiceException;
import com.gagan.catalog.repository.CategoryRepository;
import com.gagan.catalog.repository.ProductRepository;
import com.gagan.catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String normalizedSku = request.getSku().trim().toUpperCase();

        if (productRepository.existsBySkuIgnoreCase(normalizedSku)) {
            throw new CatalogServiceException(ErrorCodeEnum.SKU_ALREADY_EXISTS);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.CATEGORY_NOT_FOUND));

        Product product = Product.builder()
                .sku(normalizedSku)
                .name(request.getProductName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .isActive(true)
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        log.info("Created new product: id=[{}], sku=[{}], price=[{}], stock=[{}]",
                saved.getProductId(), saved.getSku(), saved.getPrice(), saved.getStockQuantity());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts(Long categoryId) {
        List<Product> products = (categoryId != null)
                ? productRepository.findActiveByCategoryId(categoryId)
                : productRepository.findAllActiveWithCategory();

        return products.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySkuWithCategory(sku.trim())
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));
        return mapToResponse(product);
    }
    
    @Override
    @Transactional
    public ProductResponse reserveStock(Long productId, int quantity) {
        int updatedRows = productRepository.deductStockAtomically(productId, quantity);

        if (updatedRows == 0) {
            Product existing = productRepository.findByIdWithCategory(productId)
                    .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));

            if (!Boolean.TRUE.equals(existing.getIsActive())) {
                throw new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
            }

            log.warn("Stock reservation failed for productId=[{}]: requested=[{}], available=[{}]",
                    productId, quantity, existing.getStockQuantity());
            throw new CatalogServiceException(ErrorCodeEnum.INSUFFICIENT_STOCK);
        }

        Product updatedProduct = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));

        log.info("Reserved [{}] units for productId=[{}], remainingStock=[{}]",
                quantity, productId, updatedProduct.getStockQuantity());

        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse releaseStock(Long productId, int quantity) {
        int updatedRows = productRepository.restoreStockAtomically(productId, quantity);

        if (updatedRows == 0) {
            throw new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }

        Product updatedProduct = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));

        log.info("Released [{}] units back to productId=[{}], updatedStock=[{}]",
                quantity, productId, updatedProduct.getStockQuantity());

        return mapToResponse(updatedProduct);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .sku(product.getSku())
                .productName(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .isActive(product.getIsActive())
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getName())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}