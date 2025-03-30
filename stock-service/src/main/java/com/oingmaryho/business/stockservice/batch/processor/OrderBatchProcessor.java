package com.oingmaryho.business.stockservice.batch.processor;

import com.oingmaryho.business.stockservice.application.command.StockCommandService;
import com.oingmaryho.business.stockservice.batch.buffer.OrderRequestBuffer;
import com.oingmaryho.business.stockservice.domain.event.OrderConfirmedEvent;
import com.oingmaryho.business.stockservice.domain.event.OrderCreatedEvent;
import com.oingmaryho.business.stockservice.domain.event.OrderFailedEvent;
import com.oingmaryho.business.stockservice.domain.event.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBatchProcessor {

    private final OrderRequestBuffer buffer;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StockCommandService stockCommandService;


    @Scheduled(fixedRate = 500) // 0.5초마다 처리
    public void processBatch() {
        List<OrderCreatedEvent> batch = buffer.drainBatch(10); // 최대 10개씩 처리
        if (batch.isEmpty()) return;

        log.info("📦 주문 배치 처리 시작 ({}건)", batch.size());
        List<OrderConfirmedEvent> confirmedEvents = new ArrayList<>();

        for (OrderCreatedEvent event : batch) {
            boolean allReserved = true;

            for (OrderItem item : event.items()) {
                boolean reserved = stockCommandService.reserveStock(
                    item.optionCode(), item.quantity(), event.orderId());

                if (!reserved) {
                    allReserved = false;
                    break;
                }
            }

            if (allReserved) {
                confirmedEvents.add(new OrderConfirmedEvent(event.orderId(), event.items()));
                log.info("✅ 재고 선점 성공 → order.confirmed 발행: {}", event.orderId());
            } else {
                for (OrderItem item : event.items()) {
                    stockCommandService.rollbackReservation(item.optionCode(), event.orderId());
                }
                kafkaTemplate.send("order.failed", new OrderFailedEvent(event.orderId(), "재고 부족"));
                log.warn("❌ 재고 선점 실패 → order.failed 발행: {}", event.orderId());
            }
        }
        stockCommandService.confirmAllStocks(confirmedEvents);
    }
}
