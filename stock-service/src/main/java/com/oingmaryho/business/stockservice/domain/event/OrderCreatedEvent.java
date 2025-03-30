package com.oingmaryho.business.stockservice.domain.event;

import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
    UUID orderId,
    List<OrderItem> items
) {
}