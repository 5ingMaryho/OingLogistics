package com.oingmaryho.business.delivery_service.application.dto.response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DeliveryCreationResponseServiceDto(
        UUID orderId,
        UUID orderDetailId,
        UUID deliveryId,
        String deliveryDepartureName,
        String deliveryStopoverNames,
        String deliveryDestinationName,
        String deliveryManagerName,
        String deliveryManagerSlackId
){ }
