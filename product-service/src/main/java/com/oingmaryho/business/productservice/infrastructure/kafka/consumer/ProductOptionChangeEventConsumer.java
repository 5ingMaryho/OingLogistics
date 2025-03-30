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
public class ProductOptionChangeEventConsumer {

    private final ObjectMapper objectMapper;
    private final RedisReadModelRepository redisRepo;

    @KafkaListener(topics = "stockdb.soupang.p_product_option", groupId = "product-service")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode after = root.path("payload").path("after");

            if (after.isMissingNode() || after.isNull()) {
                log.info("🗑️ 옵션 삭제 감지 → 삭제 생략 가능");
                return;
            }

            String optionCode = after.get("option_code").asText();
            UUID productId = UUID.fromString(after.get("product_id").asText());
            String optionName = after.get("option_name").asText();
            int extraPrice = after.get("option_price").asInt();

            // 기존 ReadModel 가져오기
            ProductReadModel model = redisRepo.findByProductId(productId);

            if (model == null) {
                // 최초 생성
                model = ProductReadModel.builder()
                    .productId(productId)
                    .name("") // 추후 product 테이블에서 덮어씀
                    .basePrice(0)
                    .options(new ArrayList<>())
                    .build();
            }
            // 옵션 추가 또는 수정
            List<ProductOption> updatedOptions = new ArrayList<>(model.getOptions());
            updatedOptions.removeIf(opt -> opt.getOptionCode().equals(optionCode));
            updatedOptions.add(ProductOption.builder()
                .optionCode(optionCode)
                .optionName(optionName)
                .extraPrice(extraPrice)
                .quantity(0) // 추후 p_stock에서 채워짐
                .build());

            model.setOptions(updatedOptions);
            redisRepo.save(model);

        } catch (Exception e) {
            log.error("❌ ProductOption 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}
