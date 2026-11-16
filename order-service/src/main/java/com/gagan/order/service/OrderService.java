package com.gagan.order.service;

import com.gagan.order.client.CatalogClient;
import com.gagan.order.constant.OrderErrorCodeEnum;
import com.gagan.order.dto.CheckoutRequestDto;
import com.gagan.order.dto.OrderItemRequestDto;
import com.gagan.order.dto.OrderResponseDto;
import com.gagan.order.entity.Order;
import com.gagan.order.entity.OrderItem;
import com.gagan.order.exception.OrderServiceException;
import com.gagan.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;

    @Transactional
    public OrderResponseDto checkout(String authHeader, CheckoutRequestDto requestDto) {
        // 1. Check for duplicate order reference (Idempotency check)
        if (orderRepository.findByOrderReference(requestDto.getOrderReference()).isPresent()) {
            throw new OrderServiceException(OrderErrorCodeEnum.ORDER_ALREADY_EXISTS);
        }

        // 2. Initialize Order Entity in PENDING state
        Order order = Order.builder()
                .orderReference(requestDto.getOrderReference())
                .customerEmail(requestDto.getCustomerEmail())
                .shippingAddress(requestDto.getShippingAddress())
                .orderStatus("PENDING")
                .paymentStatus("PENDING")
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal calculatedTotal = BigDecimal.ZERO;

        for (OrderItemRequestDto itemDto : requestDto.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(itemDto.getProductId())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(BigDecimal.valueOf(100.00)) // Placeholder unit price fetched from catalog in full flow
                    .build();

            calculatedTotal = calculatedTotal.add(orderItem.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
            order.getItems().add(orderItem);
        }

        order.setTotalAmount(calculatedTotal);
        Order savedOrder = orderRepository.save(order);

        // 3. Coordinate stock reservation via CatalogClient (Saga Step 1)
        try {
            for (OrderItem item : savedOrder.getItems()) {
                Map<String, Object> stockRequest = new HashMap<>();
                stockRequest.put("quantity", item.getQuantity());
                stockRequest.put("orderReference", savedOrder.getOrderReference());

                catalogClient.reserveStock(authHeader, item.getProductId(), stockRequest);
            }
        } catch (Exception ex) {
            log.error("Stock reservation failed for order reference: {}. Reason: {}", requestDto.getOrderReference(), ex.getMessage());
            savedOrder.setOrderStatus("CANCELLED");
            savedOrder.setPaymentStatus("FAILED");
            orderRepository.save(savedOrder);
            throw new OrderServiceException(OrderErrorCodeEnum.INSUFFICIENT_STOCK_FOR_CHECKOUT);
        }

        // 4. Simulate payment processing & finalize confirmation
        try {
            // Simulated successful payment gateway charge
            savedOrder.setOrderStatus("CONFIRMED");
            savedOrder.setPaymentStatus("COMPLETED");
            orderRepository.save(savedOrder);
        } catch (Exception ex) {
            log.error("Payment failed for order reference: {}. Triggering compensation rollback.", requestDto.getOrderReference());
            
            // Compensation Rollback: Release reserved stock
            for (OrderItem item : savedOrder.getItems()) {
                try {
                    Map<String, Object> releaseRequest = new HashMap<>();
                    releaseRequest.put("quantity", item.getQuantity());
                    releaseRequest.put("orderReference", savedOrder.getOrderReference());
                    catalogClient.releaseStock(authHeader, item.getProductId(), releaseRequest);
                } catch (Exception rollbackEx) {
                    log.error("Critical: Failed to release stock for product ID {} during compensation!", item.getProductId());
                }
            }

            savedOrder.setOrderStatus("CANCELLED");
            savedOrder.setPaymentStatus("FAILED");
            orderRepository.save(savedOrder);
            throw new OrderServiceException(OrderErrorCodeEnum.PAYMENT_PROCESSING_FAILED);
        }

        // 5. Final successful return statement
        return mapToResponseDto(savedOrder);
    }

    private OrderResponseDto mapToResponseDto(Order order) {
        List<OrderResponseDto.OrderItemResponseDto> itemResponseDtos = order.getItems().stream()
                .map(item -> OrderResponseDto.OrderItemResponseDto.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .orderReference(order.getOrderReference())
                .customerEmail(order.getCustomerEmail())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .shippingAddress(order.getShippingAddress())
                .items(itemResponseDtos)
                .createdAt(order.getCreatedAt())
                .build();
    }
}