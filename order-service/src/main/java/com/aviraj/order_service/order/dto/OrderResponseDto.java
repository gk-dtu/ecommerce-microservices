package com.aviraj.order_service.order.dto;

import lombok.Data;

@Data
public class OrderResponseDto {

    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private double totalPrice;
}