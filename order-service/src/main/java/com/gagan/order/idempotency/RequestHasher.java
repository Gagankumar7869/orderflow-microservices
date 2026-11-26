package com.gagan.order.idempotency;

import com.gagan.order.constant.OrderErrorCode;
import com.gagan.order.dto.CreateOrderRequest;
import com.gagan.order.dto.OrderItemRequest;
import com.gagan.order.exception.OrderServiceException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RequestHasher {

    public String hash(CreateOrderRequest request) {
        String canonicalPayload = buildCanonicalPayload(request);
        return sha256Hex(canonicalPayload);
    }

    String buildCanonicalPayload(CreateOrderRequest request) {
        String normalizedEmail = request.customerEmail().trim().toLowerCase(Locale.ROOT);
        String normalizedAddress = request.shippingAddress().trim().replaceAll("\\s+", " ");

        Set<Long> seenProductIds = new HashSet<>();
        for (OrderItemRequest item : request.items()) {
            if (!seenProductIds.add(item.productId())) {
                throw new OrderServiceException(
                        OrderErrorCode.VALIDATION_ERROR,
                        "Duplicate productId in order items: " + item.productId()
                );
            }
        }

        List<OrderItemRequest> sortedItems = request.items().stream()
                .sorted(Comparator.comparing(OrderItemRequest::productId))
                .toList();

        String canonicalItems = sortedItems.stream()
                .map(item -> item.productId() + ":" + item.quantity())
                .collect(Collectors.joining(","));

        return "email=" + normalizedEmail
                + "|address=" + normalizedAddress
                + "|items=" + canonicalItems;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}