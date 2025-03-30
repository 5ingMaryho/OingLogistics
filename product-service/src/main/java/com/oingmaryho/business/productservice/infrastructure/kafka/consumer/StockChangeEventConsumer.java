package com.oingmaryho.business.productservice.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oingmaryho.business.productservice.domain.model.ProductReadModel;
import com.oingmaryho.business.productservice.domain.model.ProductReadModel.ProductOption;
import com.oingmaryho.business.productservice.infrastructure.redis.RedisReadModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockChangeEventConsumer {

    private final ObjectMapper objectMapper;
    private final RedisReadModelRepository redisRepo;

    @KafkaListener(topics = "stockdb.soupang.p_stock", groupId = "product-service")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode after = root.path("payload").path("after");

            if (after.isMissingNode() || after.isNull()) {
                log.info("🗑️ 재고 삭제 감지 → 무시");
                return;
            }

            String optionCode = after.get("option_code").asText();
            int quantity = after.get("stock").asInt();

            // ✅ optionCode → productId 역매핑 조회
            String productIdStr = redisRepo.getProductIdByOptionCode(optionCode);
            if (productIdStr == null) {
                log.warn("⚠️ 옵션 → 상품 매핑 없음: {}", optionCode);
                return;
            }

            UUID productId = UUID.fromString(productIdStr);
            ProductReadModel model = redisRepo.findByProductId(productId);

            if (model == null) {
                log.warn("⚠️ productId={} 에 대한 ReadModel 없음", productId);
                return;
            }

            List<ProductOption> updatedOptions = new ArrayList<>();
            for (ProductOption option : model.getOptions()) {
                if (option.getOptionCode().equals(optionCode)) {
                    updatedOptions.add(ProductOption.builder()
                        .optionCode(option.getOptionCode())
                        .optionName(option.getOptionName())
                        .extraPrice(option.getExtraPrice())
                        .quantity(quantity) // ✅ 재고 반영
                        .build());
                } else {
                    updatedOptions.add(option);
                }
            }

            model.setOptions(updatedOptions);
            redisRepo.save(model);

        } catch (Exception e) {
            log.error("❌ Stock 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}
