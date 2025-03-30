package com.oingmaryho.business.productservice.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oingmaryho.business.productservice.domain.model.ProductReadModel;
import com.oingmaryho.business.productservice.infrastructure.redis.RedisReadModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductChangeEventConsumer {

    private final ObjectMapper objectMapper;
    private final RedisReadModelRepository redisRepo;

    @KafkaListener(topics = "stockdb.soupang.p_product", groupId = "product-service")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            String rawValue = record.value();

            if (rawValue == null) {
                log.info("🪦 Kafka tombstone message (null payload) 수신 → 무시");
                return;
            }

            JsonNode root = objectMapper.readTree(rawValue);
            JsonNode after = root.path("payload").path("after");

            if (after.isMissingNode() || after.isNull()) {
                UUID productId = UUID.fromString(root.path("payload").path("before").get("id").asText());
                redisRepo.delete(productId);
                log.info("🗑️ 상품 삭제 감지 → Redis에서 제거 완료: {}", productId);
                return;
            }

            UUID productId = UUID.fromString(after.get("id").asText());
            String name = after.get("name").asText();
            int basePrice = after.get("base_price").asInt();

            ProductReadModel updated = ProductReadModel.builder()
                .productId(productId)
                .name(name)
                .basePrice(basePrice)
                .options(List.of()) // 옵션은 별도 consumer에서 병합됨
                .build();

            redisRepo.save(updated);
            log.info("✅ 상품 정보 반영 완료: {}", productId);

        } catch (Exception e) {
            log.error("❌ Product 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}
