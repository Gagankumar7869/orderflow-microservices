package com.gagan.order.repository;

import com.gagan.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderReference(String orderReference);
    List<Order> findByCustomerEmail(String customerEmail);
}