package com.aviraj.order_service.producer;

import com.aviraj.order_service.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    private static final String TOPIC = "order.placed";

    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        // Key = orderId as String
        // Kafka uses key to determine which partition message goes to
        // Same orderId always goes to same partition → ordered processing
        String key = String.valueOf(event.getOrderId());

        CompletableFuture<SendResult<String, OrderPlacedEvent>> future =
                kafkaTemplate.send(TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // Success — log partition and offset for traceability
                log.info("OrderPlacedEvent published successfully | " +
                                "orderId={} | topic={} | partition={} | offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                // Failure — log but don't throw (async, order already saved)
                log.error("Failed to publish OrderPlacedEvent | orderId={} | error={}",
                        event.getOrderId(), ex.getMessage());
            }
        });
    }
}
