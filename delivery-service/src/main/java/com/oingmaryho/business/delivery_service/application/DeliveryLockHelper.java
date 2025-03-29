package com.oingmaryho.business.delivery_service.application;

import com.oingmaryho.business.delivery_service.application.service.RedisLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryLockHelper {

    private final RedisLockService redisLockService;

    /**
     * 허브 배송 담당자 락 획득
     */
    public boolean tryHubManagerLock(String lockValue, long timeoutSeconds) {
        return redisLockService.tryLock(getHubLockKey(), lockValue, timeoutSeconds);
    }

    /**
     * 업체 배송 담당자 락 획득
     */
    public boolean tryCompanyManagerLock(UUID hubId, String lockValue, long timeoutSeconds) {
        return redisLockService.tryLock(getCompanyLockKey(hubId), lockValue, timeoutSeconds);
    }

    /**
     * 락 해제
     */
    public void releaseHubManagerLock(String lockValue) {
        redisLockService.releaseLock("lock:delivery:hub", lockValue);
    }

    public void releaseCompanyManagerLock(UUID hubId, String lockValue) {
        redisLockService.releaseLock("lock:delivery:company:" + hubId, lockValue);
    }

    private String getHubLockKey() {
        return "lock:delivery:hub";
    }

    private String getCompanyLockKey(UUID hubId) {
        return "lock:delivery:company" + hubId;
    }

    public String getHubDeliveryManagerSequenceKey() {
        return "hub:delivery:sequence";
    }

    public String getCompanyDeliveryManagerSequenceKey(UUID hubId) {
        return "company:delivery:sequence:" + hubId;
    }
}
