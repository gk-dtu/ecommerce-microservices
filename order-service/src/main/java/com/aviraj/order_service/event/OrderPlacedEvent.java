package com.aviraj.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    // Event metadata
    private String eventId;        // unique ID for this event (idempotency)
    private LocalDateTime eventTime; // when event was created

    // Order data
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;

    // Username from JWT X-User-Name header
    private String placedBy;
}
