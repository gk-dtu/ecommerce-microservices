package com.aviraj.order_service.order.repository;

import com.aviraj.order_service.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}