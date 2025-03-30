package com.oingmaryho.business.stockservice.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oingmaryho.business.stockservice.application.command.StockCommandService;
import com.oingmaryho.business.stockservice.domain.event.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmedEventConsumer {

    private final ObjectMapper objectMapper;
    private final StockCommandService stockCommandService;

    @KafkaListener(topics = "order.confirmed", groupId = "stock-service")
    public void listen(ConsumerRecord<String, String> record) {
        try {
            OrderConfirmedEvent event = objectMapper.readValue(record.value(), OrderConfirmedEvent.class);

            // 여러 주문을 묶어 처리하는 구조와 호환되도록 List 로 감쌈
            List<OrderConfirmedEvent> events = new ArrayList<>();
            events.add(event);

            stockCommandService.confirmAllStocks(events);
            log.info("🧾 주문 확정 처리 완료: {}", event.orderId());

        } catch (Exception e) {
            log.error("❌ order.confirmed 수신 처리 실패: {}", e.getMessage(), e);
        }
    }
}
