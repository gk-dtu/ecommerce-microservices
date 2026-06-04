package com.aviraj.order_service.order.service;

import com.aviraj.order_service.common.exception.OrderNotFoundException;
import com.aviraj.order_service.common.exception.ResourceNotFoundException;
import com.aviraj.order_service.event.OrderPlacedEvent;
import com.aviraj.order_service.order.client.*;
import com.aviraj.order_service.order.dto.OrderRequestDto;
import com.aviraj.order_service.order.dto.OrderResponseDto;
import com.aviraj.order_service.order.dto.ProductResponseDto;
import com.aviraj.order_service.order.dto.UserResponseDto;
import com.aviraj.order_service.order.entity.Order;
import com.aviraj.order_service.order.mapper.OrderMapper;
import com.aviraj.order_service.order.repository.OrderRepository;
import com.aviraj.order_service.producer.OrderEventProducer;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderMapper orderMapper;
    private final OrderFeignService orderFeignService;
    private final OrderEventProducer orderEventProducer;
    private final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Transactional
    public OrderResponseDto placeOrder(OrderRequestDto dto) {
        logger.info("Placing order for userId={} productId={}", dto.getUserId(), dto.getProductId());

        // ── Validate user via user-service ────────────────────────
        UserResponseDto user;
        try {
            user = orderFeignService.getUser(dto.getUserId());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException(
                    "User not found with id: " + dto.getUserId());
        }

        // ── Validate product via product-service ──────────────────
        ProductResponseDto product;
        try {
            product = orderFeignService.getProduct(dto.getProductId());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException(
                    "Product not found with id: " + dto.getProductId());
        }

        // ── Calculate total price ─────────────────────────────────
        double totalPrice = product.getPrice() * dto.getQuantity();

        // ── Save order to DB ──────────────────────────────────────
        Order order = new Order();
        order.setUserId(user.getId());
        order.setProductId(product.getId());
        order.setQuantity(dto.getQuantity());
        order.setTotalPrice(totalPrice);

        Order saved = orderRepo.save(order);
        logger.info("Order saved successfully with orderId={}", saved.getId());

        // ── Publish Kafka event (async — after DB save) ───────────
        // Important: event published AFTER successful DB save
        // If Kafka is down → order is still saved → no data loss
        // Kafka failure is logged but doesn't affect API response
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID().toString())  // unique event ID
                .eventTime(LocalDateTime.now())
                .orderId(saved.getId())
                .userId(saved.getUserId())
                .productId(saved.getProductId())
                .quantity(saved.getQuantity())
                .totalAmount(BigDecimal.valueOf(saved.getTotalPrice()))
                .status("PLACED")
                .placedBy(dto.getPlacedBy())            // from JWT X-User-Name
                .build();

        orderEventProducer.publishOrderPlacedEvent(event);

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
