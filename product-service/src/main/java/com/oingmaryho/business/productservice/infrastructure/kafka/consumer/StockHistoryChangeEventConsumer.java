package com.oingmaryho.business.productservice.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockHistoryChangeEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "stockdb.soupang.p_stock_history", groupId = "product-service")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode after = root.path("payload").path("after");

            if (after.isMissingNode() || after.isNull()) {
                log.info("🗑️ 히스토리 삭제 감지 → 생략 가능");
                return;
            }

            String optionCode = after.get("option_code").asText();
            String changeType = after.get("change_type").asText(); // e.g. "INBOUND"
            int qty = after.get("change_quantity").asInt();
            String reason = after.get("reason").asText();

            log.info("📌 [재고이력] {} 옵션 {}개 변경, 사유: {}", optionCode, qty, reason);

            // ✨ 이 데이터를 저장하거나 로그로만 출력할 수도 있음
            // - Redis에 이력 저장 (key: stock:history:{optionCode})
            // - DB에 별도 적재
            // - Prometheus/InfluxDB 집계용 처리

        } catch (Exception e) {
            log.error("❌ StockHistory 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}

