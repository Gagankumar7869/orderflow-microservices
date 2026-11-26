package com.gagan.order.idempotency;

import com.gagan.order.constant.OrderErrorCode;
import com.gagan.order.dto.CreateOrderRequest;
import com.gagan.order.dto.OrderItemRequest;
import com.gagan.order.entity.IdempotencyRecord;
import com.gagan.order.enums.IdempotencyStatus;
import com.gagan.order.exception.OrderServiceException;
import com.gagan.order.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class IdempotencyServiceIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private final List<Long> createdRecordIds = Collections.synchronizedList(new ArrayList<>());

    @AfterEach
    void tearDown() {
        if (!createdRecordIds.isEmpty()) {
            idempotencyRecordRepository.deleteAllById(createdRecordIds);
            createdRecordIds.clear();
        }
    }

    @Test
    void shouldCreateNewProcessingClaimWithStableOrderReference() {
        String key = "test-key-" + UUID.randomUUID();
        CreateOrderRequest request = sampleRequest(101L, 2);

        IdempotencyResult result = idempotencyService.claimOrRetrieve(9001L, key, request);
        createdRecordIds.add(result.recordId());

        assertThat(result.outcome()).isEqualTo(IdempotencyResult.Outcome.NEW_CLAIM);
        assertThat(result.orderReference()).startsWith("ORD-");

        IdempotencyRecord saved = idempotencyRecordRepository.findById(result.recordId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
        assertThat(saved.getOrderReference()).isEqualTo(result.orderReference());
    }

    @Test
    void shouldRejectSameKeyWhileProcessingWithCheckoutInProgress() {
        String key = "test-key-" + UUID.randomUUID();
        CreateOrderRequest request = sampleRequest(101L, 2);

        IdempotencyResult first = idempotencyService.claimOrRetrieve(9001L, key, request);
        createdRecordIds.add(first.recordId());

        assertThatThrownBy(() -> idempotencyService.claimOrRetrieve(9001L, key, request))
                .isInstanceOf(OrderServiceException.class)
                .satisfies(ex -> assertThat(((OrderServiceException) ex).getErrorCode())
                        .isEqualTo(OrderErrorCode.CHECKOUT_IN_PROGRESS));
    }

    @Test
    void shouldRejectSameKeyWithModifiedPayloadAsConflict() {
        String key = "test-key-" + UUID.randomUUID();
        CreateOrderRequest firstRequest = sampleRequest(101L, 2);
        CreateOrderRequest modifiedRequest = sampleRequest(101L, 5);

        IdempotencyResult first = idempotencyService.claimOrRetrieve(9001L, key, firstRequest);
        createdRecordIds.add(first.recordId());

        assertThatThrownBy(() -> idempotencyService.claimOrRetrieve(9001L, key, modifiedRequest))
                .isInstanceOf(OrderServiceException.class)
                .satisfies(ex -> assertThat(((OrderServiceException) ex).getErrorCode())
                        .isEqualTo(OrderErrorCode.IDEMPOTENCY_CONFLICT));
    }

    @Test
    void shouldIsolateSameKeyAcrossDifferentUsers() {
        String sharedKey = "shared-key-" + UUID.randomUUID();
        CreateOrderRequest request = sampleRequest(101L, 2);

        IdempotencyResult userOneClaim = idempotencyService.claimOrRetrieve(9001L, sharedKey, request);
        IdempotencyResult userTwoClaim = idempotencyService.claimOrRetrieve(9002L, sharedKey, request);
        createdRecordIds.add(userOneClaim.recordId());
        createdRecordIds.add(userTwoClaim.recordId());

        assertThat(userOneClaim.recordId()).isNotEqualTo(userTwoClaim.recordId());
        assertThat(userOneClaim.orderReference()).isNotEqualTo(userTwoClaim.orderReference());
    }

    @Test
    void shouldTransitionProcessingToCompletedAndRejectCompletedToFailed() {
        String key = "test-key-" + UUID.randomUUID();
        CreateOrderRequest request = sampleRequest(101L, 2);

        IdempotencyResult claim = idempotencyService.claimOrRetrieve(9001L, key, request);
        createdRecordIds.add(claim.recordId());

        boolean completed = idempotencyService.markCompleted(claim.recordId(), 777L);
        assertThat(completed).isTrue();

        // Attempting to overwrite COMPLETED with FAILED must be rejected
        boolean failedAfterCompleted = idempotencyService.markFailed(claim.recordId(), "ORD-500-INTERNAL");
        assertThat(failedAfterCompleted).isFalse();

        // Retrying with same key + same payload returns EXISTING_COMPLETED with original orderId
        IdempotencyResult replay = idempotencyService.claimOrRetrieve(9001L, key, request);
        assertThat(replay.outcome()).isEqualTo(IdempotencyResult.Outcome.EXISTING_COMPLETED);
        assertThat(replay.orderId()).isEqualTo(777L);
        assertThat(replay.orderReference()).isEqualTo(claim.orderReference());
    }

    @Test
    void shouldAllowExactlyOneClaimUnderConcurrentRequests() throws Exception {
        String key = "concurrent-key-" + UUID.randomUUID();
        CreateOrderRequest request = sampleRequest(101L, 2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger newClaims = new AtomicInteger();
        AtomicInteger inProgressConflicts = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Runnable task = () -> {
                ready.countDown();
                try {
                    start.await();
                    IdempotencyResult result = idempotencyService.claimOrRetrieve(9001L, key, request);
                    if (result.outcome() == IdempotencyResult.Outcome.NEW_CLAIM) {
                        createdRecordIds.add(result.recordId());
                        newClaims.incrementAndGet();
                    }
                } catch (OrderServiceException ex) {
                    if (ex.getErrorCode() == OrderErrorCode.CHECKOUT_IN_PROGRESS) {
                        inProgressConflicts.incrementAndGet();
                    } else {
                        throw ex;
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            };

            Future<?> f1 = executor.submit(task);
            Future<?> f2 = executor.submit(task);

            ready.await();
            start.countDown();

            f1.get();
            f2.get();
        } finally {
            executor.shutdownNow();
        }

        assertThat(newClaims.get()).isEqualTo(1);
        assertThat(inProgressConflicts.get()).isEqualTo(1);
    }

    private CreateOrderRequest sampleRequest(Long productId, int quantity) {
        return new CreateOrderRequest(
                "gagan@orderflow.com",
                "Madhapur, Hyderabad",
                List.of(new OrderItemRequest(productId, quantity))
        );
    }
}