package com.oingmaryho.business.stockservice.infrastructure.redis;

import com.oingmaryho.business.stockservice.application.command.StockCommandService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.oingmaryho.business.stockservice.infrastructure.redis.RedisKeys.TTL_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisExpirationListener implements MessageListener {
    private final RedisMessageListenerContainer container;
    private final StockCommandService stockCommandService;

    @PostConstruct
    public void init() {
        container.addMessageListener(this, new PatternTopic("__keyevent@0__:expired"));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.info("🔁 Redis TTL 만료 감지: {}", expiredKey);

        if (expiredKey.startsWith(TTL_PREFIX)) {
            try {
                String orderIdStr = expiredKey.replace(TTL_PREFIX, "");
                UUID orderId = UUID.fromString(orderIdStr);
                stockCommandService.rollbackAllReservations(orderId);
            } catch (Exception e) {
                log.warn("TTL 롤백 중 예외 발생: {}", e.getMessage());
            }
        }
    }
}
