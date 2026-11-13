package com.gagan.catalog.service.impl;

import com.gagan.catalog.constant.ErrorCodeEnum;
import com.gagan.catalog.dto.ProductRequest;
import com.gagan.catalog.dto.ProductResponse;
import com.gagan.catalog.dto.ReleaseStockRequest;
import com.gagan.catalog.dto.StockUpdateRequest;
import com.gagan.catalog.entity.Category;
import com.gagan.catalog.entity.Product;
import com.gagan.catalog.entity.ReservationStatus;
import com.gagan.catalog.entity.StockReservation;
import com.gagan.catalog.exception.CatalogServiceException;
import com.gagan.catalog.repository.CategoryRepository;
import com.gagan.catalog.repository.ProductRepository;
import com.gagan.catalog.repository.StockReservationRepository;
import com.gagan.catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockReservationTxHelper reservationTxHelper;

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
    public ProductResponse reserveStock(Long productId, StockUpdateRequest request) {
        String orderRef = request.getOrderReference().trim();
        int quantity = request.getQuantity();
        String customerEmail = getAuthenticatedEmail();

        Optional<StockReservation> existingReservation =
                stockReservationRepository.findByOrderReferenceAndProductId(orderRef, productId);

        if (existingReservation.isPresent()) {
            return validateAndReturnExistingReservation(existingReservation.get(), productId, quantity);
        }

        try {
            Product updatedProduct = reservationTxHelper.executeNewReservation(
                    productId, orderRef, quantity, customerEmail);
            return mapToResponse(updatedProduct);
        } catch (DataIntegrityViolationException ex) {
            String rootMsg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
            if (!rootMsg.contains("uk_reservation_order_product")) {
                throw ex;
            }
            log.info("Resolved concurrent duplicate reserve-stock request for orderReference=[{}], productId=[{}]",
                    orderRef, productId);
            StockReservation winningReservation = stockReservationRepository
                    .findByOrderReferenceAndProductId(orderRef, productId)
                    .orElseThrow(() -> ex);
            return validateAndReturnExistingReservation(winningReservation, productId, quantity);
        }
    }

    @Override
    @Transactional
    public ProductResponse releaseStock(Long productId, ReleaseStockRequest request) {
        String orderRef = request.getOrderReference().trim();

        StockReservation reservation = stockReservationRepository
                .findByOrderReferenceAndProductId(orderRef, productId)
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.RESERVATION_NOT_FOUND));

        verifyReservationOwnership(reservation);

        int transitioned = stockReservationRepository.updateStatusAtomically(
                orderRef, productId, ReservationStatus.RESERVED, ReservationStatus.RELEASED);

        if (transitioned == 0) {
            StockReservation latest = stockReservationRepository
                    .findByOrderReferenceAndProductId(orderRef, productId)
                    .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.RESERVATION_NOT_FOUND));

            if (latest.getStatus() == ReservationStatus.CONFIRMED) {
                throw new CatalogServiceException(ErrorCodeEnum.RESERVATION_ALREADY_CONFIRMED);
            }
            throw new CatalogServiceException(ErrorCodeEnum.RESERVATION_ALREADY_RELEASED);
        }

        int updatedRows = productRepository.restoreStockAtomically(productId, reservation.getQuantity());
        if (updatedRows == 0) {
            throw new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }

        Product updatedProduct = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));

        log.info("Released [{}] units for orderReference=[{}], productId=[{}], updatedStock=[{}]",
                reservation.getQuantity(), orderRef, productId, updatedProduct.getStockQuantity());

        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse confirmStock(Long productId, ReleaseStockRequest request) {
        String orderRef = request.getOrderReference().trim();

        StockReservation reservation = stockReservationRepository
                .findByOrderReferenceAndProductId(orderRef, productId)
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.RESERVATION_NOT_FOUND));

        verifyReservationOwnership(reservation);

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            Product product = productRepository.findByIdWithCategory(productId)
                    .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));
            return mapToResponse(product);
        }

        int transitioned = stockReservationRepository.updateStatusAtomically(
                orderRef, productId, ReservationStatus.RESERVED, ReservationStatus.CONFIRMED);

        if (transitioned == 0) {
            StockReservation latest = stockReservationRepository
                    .findByOrderReferenceAndProductId(orderRef, productId)
                    .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.RESERVATION_NOT_FOUND));

            if (latest.getStatus() == ReservationStatus.CONFIRMED) {
                Product product = productRepository.findByIdWithCategory(productId)
                        .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));
                return mapToResponse(product);
            }
            throw new CatalogServiceException(ErrorCodeEnum.RESERVATION_ALREADY_RELEASED);
        }

        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));

        log.info("Confirmed stock reservation for orderReference=[{}], productId=[{}]", orderRef, productId);
        return mapToResponse(product);
    }

    private ProductResponse validateAndReturnExistingReservation(
            StockReservation reservation, Long productId, int requestedQuantity) {
        verifyReservationOwnership(reservation);

        if (!reservation.getQuantity().equals(requestedQuantity)) {
            throw new CatalogServiceException(ErrorCodeEnum.RESERVATION_REQUEST_MISMATCH);
        }

        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            throw new CatalogServiceException(ErrorCodeEnum.RESERVATION_ALREADY_RELEASED);
        }

        log.info("Idempotent reserve-stock replay for orderReference=[{}], productId=[{}]",
                reservation.getOrderReference(), productId);

        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));

        return mapToResponse(product);
    }

    private String getAuthenticatedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            throw new CatalogServiceException(ErrorCodeEnum.INVALID_TOKEN);
        }
        return auth.getName();
    }

    private void verifyReservationOwnership(StockReservation reservation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            throw new CatalogServiceException(ErrorCodeEnum.INVALID_TOKEN);
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !reservation.getCustomerEmail().equalsIgnoreCase(auth.getName())) {
            log.warn("Unauthorized reservation access for orderReference=[{}]: owner=[{}], caller=[{}]",
                    reservation.getOrderReference(), reservation.getCustomerEmail(), auth.getName());
            throw new CatalogServiceException(ErrorCodeEnum.ACCESS_DENIED);
        }
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