package com.oingmaryho.business.stockservice.domain.event;

public record OrderItem(
    String optionCode,
    int quantity
) {
}
