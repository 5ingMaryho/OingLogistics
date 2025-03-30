package com.oingmaryho.business.productservice.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oingmaryho.business.productservice.domain.model.ProductReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.oingmaryho.business.productservice.infrastructure.redis.RedisKeys.OPTION_PRODUCT_MAPPING_PREFIX;
import static com.oingmaryho.business.productservice.infrastructure.redis.RedisKeys.PRODUCT_KEY_PREFIX;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisReadModelRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(ProductReadModel model) {
        try {
            String key = PRODUCT_KEY_PREFIX + model.getProductId();
            String json = objectMapper.writeValueAsString(model);
            redisTemplate.opsForValue().set(key, json);
            log.info("✅ Redis에 ReadModel 저장: {}", key);

            // 옵션 ↔ 상품 매핑도 함께 저장
            model.getOptions().forEach(option -> {
                String mapKey = OPTION_PRODUCT_MAPPING_PREFIX + option.getOptionCode();
                redisTemplate.opsForValue().set(mapKey, model.getProductId().toString());
                log.debug("↪️ option → product 매핑 저장: {} → {}", option.getOptionCode(), model.getProductId());
            });

        } catch (JsonProcessingException e) {
            log.error("❌ Redis 저장 실패 (JSON 직렬화 오류): {}", e.getMessage(), e);
        }
    }

    public void delete(UUID productId) {
        String key = PRODUCT_KEY_PREFIX + productId;
        redisTemplate.delete(key);
        log.info("🗑️ Redis에서 ReadModel 삭제: {}", key);
    }

    public String getProductIdByOptionCode(String optionCode) {
        return redisTemplate.opsForValue().get(OPTION_PRODUCT_MAPPING_PREFIX + optionCode);
    }

    public ProductReadModel findByProductId(UUID productId) {
        String key = PRODUCT_KEY_PREFIX + productId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;

        try {
            return objectMapper.readValue(json, ProductReadModel.class);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    public List<ProductReadModel> findAll() {
        Set<String> keys = redisTemplate.keys(PRODUCT_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return List.of();

        return keys.stream()
            .map(key -> {
                String json = redisTemplate.opsForValue().get(key);
                if (json == null) return null;
                try {
                    return objectMapper.readValue(json, ProductReadModel.class);
                } catch (JsonProcessingException e) {
                    log.error("❌ JSON 역직렬화 실패 (key={}): {}", key, e.getMessage());
                    return null;
                }
            })
            .filter(model -> model != null)
            .collect(Collectors.toList());
    }
}
