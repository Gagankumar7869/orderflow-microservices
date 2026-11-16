package com.gagan.order.controller;

import com.gagan.order.dto.CheckoutRequestDto;
import com.gagan.order.dto.OrderResponseDto;
import com.gagan.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDto> checkout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CheckoutRequestDto requestDto) {
        
        OrderResponseDto responseDto = orderService.checkout(authHeader, requestDto);
        return new ResponseEntity<OrderResponseDto>(responseDto, HttpStatus.CREATED);
    }
}