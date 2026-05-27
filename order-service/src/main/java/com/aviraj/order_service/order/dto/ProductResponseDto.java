package com.aviraj.order_service.order.dto;

import lombok.Data;

@Data
public class ProductResponseDto {

    private Long id;
    private String name;
    private double price;

}
