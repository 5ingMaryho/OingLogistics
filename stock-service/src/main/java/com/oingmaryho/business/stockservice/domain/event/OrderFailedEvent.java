package com.oingmaryho.business.stockservice.domain.event;

import java.util.UUID;

public record OrderFailedEvent(
    UUID orderId,
    String reason
) {
}
