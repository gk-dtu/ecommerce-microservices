package com.aviraj.order_service.order.service;

import com.aviraj.order_service.common.exception.OrderNotFoundException;
import com.aviraj.order_service.order.dto.OrderRequestDto;
import com.aviraj.order_service.order.dto.OrderResponseDto;
import com.aviraj.order_service.order.entity.Order;
import com.aviraj.order_service.order.mapper.OrderMapper;
import com.aviraj.order_service.order.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderMapper orderMapper;
    private final Logger logger = LoggerFactory.getLogger(OrderService.class);

    public OrderService(OrderRepository orderRepo, OrderMapper orderMapper) {
        this.orderRepo = orderRepo;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponseDto placeOrder(OrderRequestDto dto) {
        logger.info("placing order with User id: {} and product id: {}", dto.getUserId(), dto.getProductId());

        Order order = new Order();
        order.setUserId(dto.getUserId());
        order.setProductId(dto.getProductId());
        order.setQuantity(dto.getQuantity());

        Order saved = orderRepo.save(order);
        logger.info("Order Placed successfully with order id: {}", saved.getId());
        return orderMapper.toOrderResponseDto(saved);
    }

    public List<OrderResponseDto> getAllOrders() {
        return orderRepo.findAll()
                .stream()
                .map(orderMapper::toOrderResponseDto)
                .toList();
    }

    public OrderResponseDto getOrderById(Long id) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        return orderMapper.toOrderResponseDto(order);
    }
}