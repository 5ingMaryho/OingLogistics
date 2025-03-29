package com.oingmaryho.business.delivery_service.application.dto.request;

import java.util.UUID;

public record OrderMessageCreationRequestServiceDto(
        UUID deliveryId
) {
}
