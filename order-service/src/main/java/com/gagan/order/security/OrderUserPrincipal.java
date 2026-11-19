package com.gagan.order.security;

public record OrderUserPrincipal(
        Long userId,
        String email,
        String role
) {}