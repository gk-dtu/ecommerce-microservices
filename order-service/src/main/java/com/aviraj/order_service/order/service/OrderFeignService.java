package com.aviraj.order_service.order.service;

import com.aviraj.order_service.common.exception.ServiceUnavailableException;
import com.aviraj.order_service.order.client.ProductClient;
import com.aviraj.order_service.order.client.UserClient;
import com.aviraj.order_service.order.dto.ProductResponseDto;
import com.aviraj.order_service.order.dto.UserResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderFeignService {

    private final UserClient userClient;
    private final ProductClient productClient;
    private final Logger logger = LoggerFactory.getLogger(OrderFeignService.class);

    @CircuitBreaker(name = "userClient", fallbackMethod = "userServiceFallback")
    public UserResponseDto getUser(Long userId) {
        return userClient.getUserById(userId);
    }

    @CircuitBreaker(name = "productClient", fallbackMethod = "productServiceFallback")
    public ProductResponseDto getProduct(Long productId) {
        return productClient.getProductById(productId);
    }

    public UserResponseDto userServiceFallback(Long userId, Throwable ex) {
        logger.error("user-service unavailable: {}", ex.getMessage());
        throw new ServiceUnavailableException("User service is currently unavailable. Please try again later.");
    }

    public ProductResponseDto productServiceFallback(Long productId, Throwable ex) {
        logger.error("product-service unavailable: {}", ex.getMessage());
        throw new ServiceUnavailableException("Product service is currently unavailable. Please try again later.");
    }
}