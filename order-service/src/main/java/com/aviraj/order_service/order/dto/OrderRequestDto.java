package com.aviraj.order_service.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class OrderRequestDto {

    @NotNull(message = "UserId is required")
    private Long userId;

    @NotNull(message = "ProductId is required")
    private Long productId;

    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;
}