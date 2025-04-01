package com.oingmaryho.business.delivery_service.infrastructure.adapter;

import com.oingmaryho.business.delivery_service.application.dto.request.*;
import com.oingmaryho.business.delivery_service.application.service.DeliveryAdminService;
import com.oingmaryho.business.delivery_service.presentation.dto.request.DeliveryCreationRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final DeliveryAdminService deliveryAdminService;

    @RabbitListener(queues = "${message.queue.delivery}")
    public void createDelivery(DeliveryCreationRequestDto requestDto) {
        DeliveryCreationRequestServiceDto requestServiceDto = new DeliveryCreationRequestServiceDto(
                requestDto.orderId(),
                requestDto.orderDetailId(),
                requestDto.recipientId(),
                requestDto.requesterAddress(),
                requestDto.requesterName(),
                requestDto.requesterSlackId(),
                requestDto.recipientHubId()
        );
        deliveryAdminService.createDelivery(requestServiceDto);
    }
}