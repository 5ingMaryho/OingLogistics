package com.oingmaryho.business.delivery_service.application;

import com.oingmaryho.business.delivery_service.application.dto.request.DeliveryManagerAssignmentRequestServiceDto;
import com.oingmaryho.business.delivery_service.application.dto.request.OrderMessageCreationRequestServiceDto;
import com.oingmaryho.business.delivery_service.application.service.DeliveryLockService;
import com.oingmaryho.business.delivery_service.domain.entity.Delivery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryManagerAssignmentHelper {

    private final DeliveryLockService deliveryLockService;

    public String getHubDeliveryManagerLockKey() {
        return "hub:delivery";
    }

    public String getCompanyDeliveryManagerLockKey(UUID hubId) {
        return "company:delivery:" + hubId;
    }

    public String getHubDeliveryManagerSequenceKey() {
        return "hub:delivery:sequence";
    }

    public String getCompanyDeliveryManagerSequenceKey(UUID hubId) {
        return "company:delivery:sequence:" + hubId;
    }

    public DeliveryManagerAssignmentRequestServiceDto assignHubDeliveryManagerWithLock(
            String lockKey, String sequenceKey, Delivery delivery) {
        return deliveryLockService.assignHubDeliveryManagerWithLock(lockKey, sequenceKey, delivery);
    }

    public OrderMessageCreationRequestServiceDto assignCompanyDeliveryManagerWithLock(
            String lockKey, String sequenceKey, Delivery delivery, UUID arriveHubId) {
        return deliveryLockService.assignCompanyDeliveryManagerWithLock(lockKey, sequenceKey, delivery, arriveHubId);
    }
}
