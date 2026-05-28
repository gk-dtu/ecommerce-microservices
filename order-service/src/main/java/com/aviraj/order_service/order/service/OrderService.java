package com.aviraj.order_service.order.service;

import com.aviraj.order_service.common.exception.OrderNotFoundException;
import com.aviraj.order_service.common.exception.ResourceNotFoundException;
import com.aviraj.order_service.order.client.*;
import com.aviraj.order_service.order.dto.OrderRequestDto;
import com.aviraj.order_service.order.dto.OrderResponseDto;
import com.aviraj.order_service.order.dto.ProductResponseDto;
import com.aviraj.order_service.order.dto.UserResponseDto;
import com.aviraj.order_service.order.entity.Order;
import com.aviraj.order_service.order.mapper.OrderMapper;
import com.aviraj.order_service.order.repository.OrderRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderMapper orderMapper;
    private final OrderFeignService orderFeignService;
    private final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Transactional
    public OrderResponseDto placeOrder(OrderRequestDto dto) {
        logger.info("placing order with User id: {} and product id: {}", dto.getUserId(), dto.getProductId());

        UserResponseDto user;
        ProductResponseDto product;
        // 🔥 Validate user via user-service
        try{
            user = orderFeignService.getUser(dto.getUserId());
        }catch (FeignException.NotFound e){
            throw new ResourceNotFoundException("User not found with id: " + dto.getUserId());
        }
        // 🔥 Validate product via product-service

        try{
            product = orderFeignService.getProduct(dto.getProductId());
        }catch (FeignException.NotFound e){
            throw new ResourceNotFoundException("Product not found with id: " + dto.getProductId());
        }

        // 🔥 Calculate total price
        double totalPrice = product.getPrice() * dto.getQuantity();

        Order order = new Order();
        order.setUserId(user.getId());
        order.setProductId(product.getId());
        order.setQuantity(dto.getQuantity());
        order.setTotalPrice(totalPrice);

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
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        return orderMapper.toOrderResponseDto(order);
    }
}