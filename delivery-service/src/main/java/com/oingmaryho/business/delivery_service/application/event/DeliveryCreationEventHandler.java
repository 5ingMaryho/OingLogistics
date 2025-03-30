package com.oingmaryho.business.delivery_service.application.event;

import com.oingmaryho.business.delivery_service.application.DeliveryManagerAssignmentHelper;
import com.oingmaryho.business.delivery_service.application.dto.request.*;
import com.oingmaryho.business.delivery_service.application.dto.response.DeliveryCreationResponseServiceDto;
import com.oingmaryho.business.delivery_service.application.service.DeliveryAdminService;
import com.oingmaryho.business.delivery_service.exception.DeliveryException;
import com.oingmaryho.business.delivery_service.exception.ErrorCode;
import com.oingmaryho.business.delivery_service.presentation.dto.request.DeliveryCreationRequestDto;
import com.oingmaryho.business.delivery_service.presentation.dto.response.DeliveryCreationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryCreationEventHandler {

    private final RabbitTemplate rabbitTemplate;
    private final DeliveryAdminService deliveryAdminService;
    private final DeliveryManagerAssignmentHelper deliveryManagerAssignmentHelper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${message.queue.order}")
    private String queueOrder;

    @Value("${message.queue.hubDeliveryManager}")
    private String queueHubDeliveryManager;

    @Value("${message.queue.companyDeliveryManager}")
    private String queueCompanyDeliveryManager;

    @Value("${message.queue.deliveryMessageCreation}")
    private String queueDeliveryMessageCreation;


    @Transactional
    @RabbitListener(queues = "queueDelivery")
    public void createDelivery(DeliveryCreationRequestDto requestDto) {

        log.info("[Delivery Creation Request] orderId = {}, orderDetailId = {}, hubId = {}, address = {}",
                requestDto.orderId(), requestDto.orderDetailId(), requestDto.recipientHubId(), requestDto.requesterAddress());

        DeliveryCreationRequestServiceDto requestServiceDto = new DeliveryCreationRequestServiceDto(
                requestDto.orderId(),
                requestDto.orderDetailId(),
                requestDto.recipientId(),
                requestDto.requesterAddress(),
                requestDto.requesterName(),
                requestDto.requesterSlackId(),
                requestDto.recipientHubId()
        );

        try {
            DeliveryManagerAssignmentRequestServiceDto responseServiceDto = deliveryAdminService.createDelivery(requestServiceDto);
            rabbitTemplate.convertAndSend(queueHubDeliveryManager, new DeliveryManagerAssignmentRequestDto(
                    responseServiceDto.deliveryId()
            ));
        } catch (DeliveryException e) {
            // TODO SAGA ? OR DLQ ?
        } catch (Exception e) {
            e.fillInStackTrace();
        }

    }

    @Transactional
    @RabbitListener(queues = "queueHubDeliveryManager")
    public void assignHubDeliveryManager(DeliveryManagerAssignmentRequestDto requestDto) {

        log.info("[HubDeliveryManager Assignment Request] deliveryId = {}", requestDto.deliveryId());

        DeliveryManagerAssignmentRequestServiceDto requestServiceDto = new DeliveryManagerAssignmentRequestServiceDto(
                requestDto.deliveryId()
        );

        try {
            DeliveryManagerAssignmentRequestServiceDto responseServiceDto = deliveryAdminService.assignHubDeliveryManager(requestServiceDto);
            rabbitTemplate.convertAndSend(queueCompanyDeliveryManager, new DeliveryManagerAssignmentRequestDto(
                    responseServiceDto.deliveryId()
            ));
        } catch (DeliveryException e) {
            // TODO SAGA ? OR DLQ ?
        } catch (Exception e) {
            e.fillInStackTrace();
        }

    }

    @Transactional
    @RabbitListener(queues = "queueCompanyDeliveryManager")
    public void assignCompanyDeliveryManager(DeliveryManagerAssignmentRequestDto requestDto) {

        log.info("[CompanyDeliveryManager Assignment Request] deliveryId = {}", requestDto.deliveryId());

        DeliveryManagerAssignmentRequestServiceDto requestServiceDto = new DeliveryManagerAssignmentRequestServiceDto(
                requestDto.deliveryId()
        );

        try {
            OrderMessageCreationRequestServiceDto responseServiceDto = deliveryAdminService.assignCompanyDeliveryManager(requestServiceDto);
            rabbitTemplate.convertAndSend(queueDeliveryMessageCreation, new OrderMessageCreationRequestDto(
                    responseServiceDto.deliveryId()
            ));
        } catch (DeliveryException e) {
            // TODO SAGA ? OR DLQ ?
        } catch (Exception e) {
            e.fillInStackTrace();
        }

    }

    @Transactional
    @RabbitListener(queues = "queueDeliveryMessageCreation")
    public void assignCompanyDeliveryManager(OrderMessageCreationRequestDto requestDto) {

        log.info("[DeliveryMessage Creation Request] deliveryId = {}", requestDto.deliveryId());

        OrderMessageCreationRequestServiceDto requestServiceDto = new OrderMessageCreationRequestServiceDto(
                requestDto.deliveryId()
        );

        try {
            DeliveryCreationResponseServiceDto responseServiceDto = deliveryAdminService.createMessageToOrder(requestServiceDto);
            rabbitTemplate.convertAndSend(queueOrder, new DeliveryCreationResponseDto(
                    responseServiceDto.orderId(),
                    responseServiceDto.orderDetailId(),
                    responseServiceDto.deliveryId(),
                    responseServiceDto.deliveryDepartureName(),
                    responseServiceDto.deliveryStopoverNames(),
                    responseServiceDto.deliveryDestinationName(),
                    responseServiceDto.deliveryManagerName(),
                    responseServiceDto.deliveryManagerSlackId())
            );
            log.info("[Delivery Creation Success Message Issued] orderId = {}, orderDetailId = {}, deliveryId = {}",
                    responseServiceDto.orderId(),responseServiceDto.orderDetailId(), responseServiceDto.deliveryId());
        } catch (DeliveryException e) {
            // TODO SAGA ? OR DLQ ?
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }


}
