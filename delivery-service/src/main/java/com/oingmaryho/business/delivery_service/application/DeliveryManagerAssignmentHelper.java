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

    private String getHubDeliveryManagerLockKey() {
        return "hub:delivery";
    }

    private String getCompanyDeliveryManagerLockKey(UUID hubId) {
        return "company:delivery:" + hubId;
    }

    private String getHubDeliveryManagerSequenceKey() {
        return "hub:delivery:sequence";
    }

    private String getCompanyDeliveryManagerSequenceKey(UUID hubId) {
        return "company:delivery:sequence:" + hubId;
    }

    public DeliveryManagerAssignmentRequestServiceDto assignHubDeliveryManagerWithLock(Delivery delivery) {
        return deliveryLockService.assignHubDeliveryManagerWithLock(
                getHubDeliveryManagerLockKey(),
                getHubDeliveryManagerSequenceKey(),
                delivery);
    }

    public OrderMessageCreationRequestServiceDto assignCompanyDeliveryManagerWithLock(Delivery delivery,
                                                                                      UUID arriveHubId) {
        return deliveryLockService.assignCompanyDeliveryManagerWithLock(
                getCompanyDeliveryManagerLockKey(arriveHubId),
                getCompanyDeliveryManagerSequenceKey(arriveHubId),
                delivery,
                arriveHubId
        );

    }
}
