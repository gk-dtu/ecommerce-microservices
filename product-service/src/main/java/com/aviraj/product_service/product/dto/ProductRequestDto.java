package com.aviraj.product_service.product.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class ProductRequestDto {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotNull(message = "price cannot be Null")
    @Positive
    private double price;

    @NotNull(message = "user_id cannot be Null")
    @Positive
    private Long userId;
}