package com.oingmaryho.business.stockservice.application.command;

import com.oingmaryho.business.stockservice.domain.event.OrderConfirmedEvent;
import com.oingmaryho.business.stockservice.domain.event.OrderItem;
import com.oingmaryho.business.stockservice.domain.model.Stock;
import com.oingmaryho.business.stockservice.domain.model.StockHistory;
import com.oingmaryho.business.stockservice.domain.model.StockHistory.ChangeType;
import com.oingmaryho.business.stockservice.domain.repository.StockHistoryRepository;
import com.oingmaryho.business.stockservice.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.oingmaryho.business.stockservice.infrastructure.redis.RedisKeys.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCommandService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StockRepository stockRepository;
    private final StockHistoryRepository stockHistoryRepository;

    /**
     * Redis에 재고 선점
     */
    // TODO: order-service 에서 order.created 이벤트 송신 로직 필요
    public boolean reserveStock(String optionCode, int quantity, UUID orderId) {
        String stockKey = STOCK_PREFIX + optionCode;
        String reservedKey = RESERVED_PREFIX + optionCode;
        String ttlKey = TTL_PREFIX + orderId;
        String orderKey = RESERVED_ORDER_PREFIX + orderId; // 추가!

        Integer current = (Integer) redisTemplate.opsForValue().get(stockKey);
        if (current == null || current < quantity) return false;

        redisTemplate.opsForValue().decrement(stockKey, quantity);
        redisTemplate.opsForHash().put(reservedKey, String.valueOf(orderId), quantity);
        redisTemplate.opsForList().rightPush(orderKey, optionCode); // ✅ orderId로 예약 옵션 저장
        redisTemplate.opsForValue().set(ttlKey, "1", Duration.ofMinutes(10));

        return true;
    }


    /**
     * 주문 확정 시 실제 차감 - 배치 처리
     */
    // TODO: 배치 처리 실패 시 모든 요청 롤백이기 때문에 서킷 브레이커처럼 실패에 대한 처리 로직 필요
    @Transactional
    public void confirmAllStocks(List<OrderConfirmedEvent> events) {
        for (OrderConfirmedEvent event : events) {
            for (OrderItem item : event.items()) {
                confirmStock(item.optionCode(), item.quantity(), event.orderId());
            }
        }
    }

    @Transactional
    public void confirmStock(String optionCode, int quantity, UUID orderId) {
        String confirmKey = CONFIRMED_PREFIX + optionCode + ":" + orderId;
        Boolean alreadyConfirmed = redisTemplate.hasKey(confirmKey);
        if (Boolean.TRUE.equals(alreadyConfirmed)) {
            log.info("⏩ 이미 confirm 처리된 주문: orderId={}, optionCode={}", orderId, optionCode);
            return;
        }

        // 실제 재고 차감 로직
        Stock stock = stockRepository.findByOptionCode(optionCode)
            .orElseThrow(() -> {
                log.warn("❌ confirm 실패: optionCode={}, orderId={}", optionCode, orderId);
                return new IllegalArgumentException("재고 정보 없음");
            });

        stock.decrease(quantity);
        stockRepository.save(stock);

        stockHistoryRepository.save(StockHistory.builder()
            .optionCode(optionCode)
            .changeType(ChangeType.DECREASE)
            .changeQuantity(quantity)
            .reason("ORDER#" + orderId)
            .build());

        redisTemplate.opsForHash().delete(RESERVED_PREFIX + optionCode, String.valueOf(orderId));
        redisTemplate.delete(TTL_PREFIX + orderId);

        // ✅ idempotent 처리 키 저장
        redisTemplate.opsForValue().set(confirmKey, "1", Duration.ofDays(1));
    }


    /**
     * 주문 실패 시 Redis 재고 복원
     */
    public void rollbackReservation(String optionCode, UUID orderId) {
        String reservedKey = RESERVED_PREFIX + optionCode;
        Object quantityObj = redisTemplate.opsForHash().get(reservedKey, String.valueOf(orderId));
        if (quantityObj == null) return;
        int quantity = Integer.parseInt(quantityObj.toString());

        redisTemplate.opsForValue().increment(STOCK_PREFIX + optionCode, quantity);
        redisTemplate.opsForHash().delete(reservedKey, String.valueOf(orderId));
        redisTemplate.delete(TTL_PREFIX + orderId);
    }

    public void rollbackAllReservations(UUID orderId) {
        String orderKey = RESERVED_ORDER_PREFIX + orderId;
        List<Object> optionCodes = redisTemplate.opsForList().range(orderKey, 0, -1);
        if (optionCodes == null || optionCodes.isEmpty()) return;

        for (Object codeObj : optionCodes) {
            String optionCode = codeObj.toString();
            String reservedKey = RESERVED_PREFIX + optionCode;

            Object quantityObj = redisTemplate.opsForHash().get(reservedKey, String.valueOf(orderId));
            if (quantityObj != null) {
                int quantity = Integer.parseInt(quantityObj.toString());
                redisTemplate.opsForValue().increment(STOCK_PREFIX + optionCode, quantity);
                redisTemplate.opsForHash().delete(reservedKey, String.valueOf(orderId));
                log.info("♻️ TTL 롤백 실행: optionCode={}, quantity={}", optionCode, quantity);
            }
        }

        redisTemplate.delete(TTL_PREFIX + orderId);
        redisTemplate.delete(orderKey); // ✅ 롤백 후 정리
    }


}
