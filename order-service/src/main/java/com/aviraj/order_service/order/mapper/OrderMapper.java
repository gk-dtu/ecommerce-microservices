package com.aviraj.order_service.order.mapper;

import com.aviraj.order_service.order.dto.OrderResponseDto;
import com.aviraj.order_service.order.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
    public OrderResponseDto toOrderResponseDto(Order order) {

        OrderResponseDto dto = new OrderResponseDto();

        dto.setId(order.getId());
        dto.setProductId(order.getProductId());
        dto.setQuantity(order.getQuantity());
        dto.setUserId(order.getUserId());
        dto.setTotalPrice(order.getTotalPrice());

        return dto;
    }
}
