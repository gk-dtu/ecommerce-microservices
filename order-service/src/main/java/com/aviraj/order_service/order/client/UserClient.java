package com.aviraj.order_service.order.client;

import com.aviraj.order_service.order.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "http://localhost:8081")
public interface UserClient {

    @GetMapping("/users/{id}")
    UserResponseDto getUserById(@PathVariable Long id);
}