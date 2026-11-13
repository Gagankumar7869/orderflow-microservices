package com.gagan.catalog;

import com.gagan.catalog.constant.ErrorCodeEnum;
import com.gagan.catalog.dto.ProductResponse;
import com.gagan.catalog.dto.ReleaseStockRequest;
import com.gagan.catalog.dto.StockUpdateRequest;
import com.gagan.catalog.entity.Category;
import com.gagan.catalog.entity.Product;
import com.gagan.catalog.entity.StockReservation;
import com.gagan.catalog.exception.CatalogServiceException;
import com.gagan.catalog.repository.CategoryRepository;
import com.gagan.catalog.repository.ProductRepository;
import com.gagan.catalog.repository.StockReservationRepository;
import com.gagan.catalog.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class StockReservationConcurrencyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    private Long testProductId;
    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder()
                .name("TestCat-" + UUID.randomUUID().toString().substring(0, 8))
                .description("Concurrency test category")
                .build());
        this.testCategoryId = category.getCategoryId();

        Product product = productRepository.save(Product.builder()
                .sku("SKU-" + UUID.randomUUID().toString().substring(0, 8))
                .name("Concurrency Test Phone")
                .description("Test item")
                .price(new BigDecimal("19999.00"))
                .stockQuantity(10)
                .isActive(true)
                .category(category)
                .build());

        this.testProductId = product.getProductId();
    }

    @AfterEach
    void tearDown() {
        List<StockReservation> testReservations = stockReservationRepository.findAll().stream()
                .filter(r -> r.getProductId().equals(testProductId))
                .toList();
        stockReservationRepository.deleteAll(testReservations);

        if (testProductId != null) {
            productRepository.deleteById(testProductId);
        }
        if (testCategoryId != null) {
            categoryRepository.deleteById(testCategoryId);
        }
    }

    private void authenticateCustomer(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                )
        );
    }

    @Test
    void concurrentCompetingOrders_ForLimitedStock_ShouldPreventOverselling() throws InterruptedException {
        // Initial stock is 10. Two different orders (ORD-001 and ORD-002) each request 7 units simultaneously.
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        String orderRefPrefix = "ORD-COMP-" + UUID.randomUUID().toString().substring(0, 6) + "-";
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);

        for (int i = 1; i <= threads; i++) {
            final String distinctOrderRef = orderRefPrefix + i;
            executor.submit(() -> {
                try {
                    authenticateCustomer("customer" + distinctOrderRef + "@orderflow.com");
                    readyLatch.countDown();
                    startLatch.await();

                    productService.reserveStock(testProductId, StockUpdateRequest.builder()
                            .orderReference(distinctOrderRef)
                            .quantity(7)
                            .build());
                    successCount.incrementAndGet();
                } catch (CatalogServiceException ex) {
                    if (ErrorCodeEnum.INSUFFICIENT_STOCK.getErrorCode().equals(ex.getErrorCode())) {
                        insufficientStockCount.incrementAndGet();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Product updated = productRepository.findById(testProductId).orElseThrow();
        assertEquals(1, successCount.get(), "Exactly one competing order must succeed");
        assertEquals(1, insufficientStockCount.get(), "The second competing order must fail with 20016 INSUFFICIENT_STOCK");
        assertEquals(3, updated.getStockQuantity(), "Remaining stock must be 10 - 7 = 3 and never negative");
    }

    @Test
    void concurrentDuplicateReservations_ShouldDeductStockOnlyOnce() throws InterruptedException {
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        String orderRef = "ORD-CONC-" + UUID.randomUUID().toString().substring(0, 8);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    authenticateCustomer("customer@orderflow.com");
                    readyLatch.countDown();
                    startLatch.await();

                    productService.reserveStock(testProductId, StockUpdateRequest.builder()
                            .orderReference(orderRef)
                            .quantity(4)
                            .build());
                    successCount.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Product updated = productRepository.findById(testProductId).orElseThrow();
        assertEquals(6, updated.getStockQuantity(), "Stock (10) should be deducted by 4 only once, leaving 6");
        assertEquals(threads, successCount.get(), "All duplicate retries should resolve idempotently");
    }

    @Test
    void concurrentReleaseRequests_ShouldRestoreStockOnlyOnce() throws InterruptedException {
        String orderRef = "ORD-REL-" + UUID.randomUUID().toString().substring(0, 8);
        authenticateCustomer("customer@orderflow.com");
        productService.reserveStock(testProductId, StockUpdateRequest.builder()
                .orderReference(orderRef)
                .quantity(4)
                .build());

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger releaseSuccessCount = new AtomicInteger(0);
        AtomicInteger alreadyReleasedCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    authenticateCustomer("customer@orderflow.com");
                    readyLatch.countDown();
                    startLatch.await();

                    productService.releaseStock(testProductId, ReleaseStockRequest.builder()
                            .orderReference(orderRef)
                            .build());
                    releaseSuccessCount.incrementAndGet();
                } catch (CatalogServiceException ex) {
                    if (ErrorCodeEnum.RESERVATION_ALREADY_RELEASED.getErrorCode().equals(ex.getErrorCode())) {
                        alreadyReleasedCount.incrementAndGet();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Product updated = productRepository.findById(testProductId).orElseThrow();
        assertEquals(1, releaseSuccessCount.get());
        assertEquals(threads - 1, alreadyReleasedCount.get());
        assertEquals(10, updated.getStockQuantity());
    }

    @Test
    void confirmedReservation_CannotBeReleased() {
        String orderRef = "ORD-CONF-" + UUID.randomUUID().toString().substring(0, 8);
        authenticateCustomer("customer@orderflow.com");

        productService.reserveStock(testProductId, StockUpdateRequest.builder()
                .orderReference(orderRef)
                .quantity(4)
                .build());

        productService.confirmStock(testProductId, ReleaseStockRequest.builder()
                .orderReference(orderRef)
                .build());

        CatalogServiceException ex = assertThrows(CatalogServiceException.class, () ->
                productService.releaseStock(testProductId, ReleaseStockRequest.builder()
                        .orderReference(orderRef)
                        .build())
        );

        assertEquals(ErrorCodeEnum.RESERVATION_ALREADY_CONFIRMED.getErrorCode(), ex.getErrorCode());
        ProductResponse afterAttempt = productService.getProductById(testProductId);
        assertEquals(6, afterAttempt.getStockQuantity());
    }
}