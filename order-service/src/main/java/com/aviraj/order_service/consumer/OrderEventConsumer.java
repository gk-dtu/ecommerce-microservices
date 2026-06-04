package com.aviraj.order_service.consumer;

import com.aviraj.order_service.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventConsumer {

    // @KafkaListener — Spring creates a consumer thread automatically
    // groupId — this consumer belongs to order-service-group
    // In real system → this would be a separate notification-service
    @KafkaListener(
            topics = "order.placed",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderPlacedEvent(
            ConsumerRecord<String, OrderPlacedEvent> record) {

        OrderPlacedEvent event = record.value();

        log.info("╔══════════════════════════════════════════");
        log.info("║ ORDER EVENT RECEIVED");
        log.info("║ Topic:     {}", record.topic());
        log.info("║ Partition: {}", record.partition());
        log.info("║ Offset:    {}", record.offset());
        log.info("║ Key:       {}", record.key());
        log.info("║ OrderId:   {}", event.getOrderId());
        log.info("║ UserId:    {}", event.getUserId());
        log.info("║ ProductId: {}", event.getProductId());
        log.info("║ Quantity:  {}", event.getQuantity());
        log.info("║ Amount:    {}", event.getTotalAmount());
        log.info("║ PlacedBy:  {}", event.getPlacedBy());
        log.info("║ EventTime: {}", event.getEventTime());
        log.info("╚══════════════════════════════════════════");

        // In real system — this is where you would:
        // → Send email/SMS notification
        // → Update inventory
        // → Trigger payment processing
        // → Update analytics dashboard
        // All without order-service knowing about these services
    }
}
