package com.oingmaryho.business.delivery_service.infrastructure.adapter;

import com.oingmaryho.business.delivery_service.application.dto.request.DeliveryManagerAssignmentRequestDto;
import com.oingmaryho.business.delivery_service.application.dto.request.DeliveryManagerAssignmentRequestServiceDto;
import com.oingmaryho.business.delivery_service.application.service.DeliveryAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryCreatedProducer {

    private final DeliveryAdminService deliveryAdminService;

    @RabbitListener(queues = "${message.queue.deliveryManager}")
    public void assignHubDeliveryManager(DeliveryManagerAssignmentRequestDto requestDto) {
        DeliveryManagerAssignmentRequestServiceDto requestServiceDto = new DeliveryManagerAssignmentRequestServiceDto(
                requestDto.deliveryId()
        );
        deliveryAdminService.assignDeliveryManager(requestServiceDto);
    }
}
