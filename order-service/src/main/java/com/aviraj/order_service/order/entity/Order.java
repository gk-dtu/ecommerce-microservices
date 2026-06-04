package com.aviraj.order_service.order.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long productId;
    private Integer quantity;
    private double totalPrice;

    // Who placed this order — from JWT identity propagation
    private String placedBy;
}
