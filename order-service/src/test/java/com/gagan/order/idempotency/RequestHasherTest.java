package com.gagan.order.idempotency;

import com.gagan.order.constant.OrderErrorCode;
import com.gagan.order.dto.CreateOrderRequest;
import com.gagan.order.dto.OrderItemRequest;
import com.gagan.order.exception.OrderServiceException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestHasherTest {

    private final RequestHasher requestHasher = new RequestHasher();

    @Test
    void shouldProduce64CharLowercaseSha256Hex() {
        CreateOrderRequest request = new CreateOrderRequest(
                "gagan@orderflow.com",
                "Madhapur, Hyderabad",
                List.of(new OrderItemRequest(101L, 2))
        );

        String hash = requestHasher.hash(request);

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void shouldProduceSameHashWhenItemsAreInDifferentOrder() {
        CreateOrderRequest requestA = new CreateOrderRequest(
                "gagan@orderflow.com",
                "Madhapur, Hyderabad",
                List.of(
                        new OrderItemRequest(101L, 2),
                        new OrderItemRequest(102L, 1)
                )
        );
        CreateOrderRequest requestB = new CreateOrderRequest(
                "gagan@orderflow.com",
                "Madhapur, Hyderabad",
                List.of(
                        new OrderItemRequest(102L, 1),
                        new OrderItemRequest(101L, 2)
                )
        );

        assertThat(requestHasher.hash(requestA)).isEqualTo(requestHasher.hash(requestB));
    }

    @Test
    void shouldNormalizeEmailCaseAndAddressWhitespace() {
        CreateOrderRequest requestA = new CreateOrderRequest(
                "  Gagan@OrderFlow.COM ",
                "  Madhapur,   Hyderabad  ",
                List.of(new OrderItemRequest(101L, 2))
        );
        CreateOrderRequest requestB = new CreateOrderRequest(
                "gagan@orderflow.com",
                "Madhapur, Hyderabad",
                List.of(new OrderItemRequest(101L, 2))
        );

        assertThat(requestHasher.hash(requestA)).isEqualTo(requestHasher.hash(requestB));
    }

    @Test
    void shouldProduceDifferentHashWhenQuantityChanges() {
        CreateOrderRequest requestA = new CreateOrderRequest(
                "gagan@orderflow.com",
                "Madhapur, Hyderabad",
                List.of(new OrderItemRequest(101L, 2))
        );
        CreateOrderRequest requestB = new CreateOrderRequest(
                "gagan@orderflow.com",
                "Madhapur, Hyderabad",
                List.of(new OrderItemRequest(101L, 3))
        );

        assertThat(requestHasher.hash(requestA)).isNotEqualTo(requestHasher.hash(requestB));
    }

    @Test
    void shouldRejectDuplicateProductIdsWithValidationError() {
        CreateOrderRequest request = new CreateOrderRequest(
                "gagan@orderflow.com",
                "Madhapur, Hyderabad",
                List.of(
                        new OrderItemRequest(101L, 2),
                        new OrderItemRequest(101L, 1)
                )
        );

        assertThatThrownBy(() -> requestHasher.hash(request))
                .isInstanceOf(OrderServiceException.class)
                .satisfies(ex -> assertThat(((OrderServiceException) ex).getErrorCode())
                        .isEqualTo(OrderErrorCode.VALIDATION_ERROR));
    }
}