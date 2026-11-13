package com.gagan.catalog.service.impl;

import com.gagan.catalog.constant.ErrorCodeEnum;
import com.gagan.catalog.entity.Product;
import com.gagan.catalog.entity.ReservationStatus;
import com.gagan.catalog.entity.StockReservation;
import com.gagan.catalog.exception.CatalogServiceException;
import com.gagan.catalog.repository.ProductRepository;
import com.gagan.catalog.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockReservationTxHelper {

    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Product executeNewReservation(Long productId, String orderRef, int quantity, String customerEmail) {
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

        StockReservation reservation = StockReservation.builder()
                .orderReference(orderRef)
                .productId(productId)
                .quantity(quantity)
                .customerEmail(customerEmail)
                .status(ReservationStatus.RESERVED)
                .build();

        stockReservationRepository.saveAndFlush(reservation);

        Product updatedProduct = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new CatalogServiceException(ErrorCodeEnum.PRODUCT_NOT_FOUND));

        log.info("Reserved [{}] units for orderReference=[{}], productId=[{}], remainingStock=[{}]",
                quantity, orderRef, productId, updatedProduct.getStockQuantity());

        return updatedProduct;
    }
}