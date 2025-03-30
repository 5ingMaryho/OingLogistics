package com.oingmaryho.business.stockservice.infrastructure.kafka.consumer;

import com.oingmaryho.business.stockservice.batch.buffer.OrderRequestBuffer;
import com.oingmaryho.business.stockservice.domain.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {

    private final OrderRequestBuffer orderRequestBuffer;

    @KafkaListener(topics = "order.created", groupId = "stock-service")
    public void listen(ConsumerRecord<String, OrderCreatedEvent> record) {
        log.info("🛒 주문 이벤트 수신 (버퍼 저장): {}", record.value());
        orderRequestBuffer.add(record.value());
    }
}
