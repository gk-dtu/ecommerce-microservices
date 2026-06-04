package com.aviraj.order_service.order.controller;

import com.aviraj.order_service.common.response.ApiResponse;
import com.aviraj.order_service.order.dto.OrderRequestDto;
import com.aviraj.order_service.order.dto.OrderResponseDto;
import com.aviraj.order_service.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<OrderResponseDto> placeOrder(
        @Valid @RequestBody OrderRequestDto dto,
        // X-User-Name set by JwtAuthFilter in gateway
        // defaultValue = "anonymous" for local dev without gateway
        @RequestHeader(value = "X-User-Name", defaultValue = "anonymous") String username) {

        // Set authenticated user into dto — not from client, from JWT
        dto.setPlacedBy(username);

        OrderResponseDto response = service.placeOrder(dto);
        return new ApiResponse<>(true, "Order Placed", response);
    }

    @GetMapping
    public List<OrderResponseDto> getAll() {
        return service.getAllOrders();
    }

    @GetMapping("/{id}")
    public OrderResponseDto getById(@PathVariable Long id) {
        return service.getOrderById(id);
    }
}
