package com.oingmaryho.business.stockservice.domain.event;

import java.util.List;
import java.util.UUID;

public record OrderConfirmedEvent(
    UUID orderId,
    List<OrderItem> items
) {
}