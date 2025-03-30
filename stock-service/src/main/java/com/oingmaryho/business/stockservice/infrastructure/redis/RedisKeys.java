package com.oingmaryho.business.stockservice.infrastructure.redis;

public class RedisKeys {
    public static final String STOCK_PREFIX = "stock:";                  // 재고 저장 키
    public static final String RESERVED_PREFIX = "reserved:";            // 선점 예약 정보 (Hash)
    public static final String TTL_PREFIX = "reserved:ttl:";             // TTL 감지용 키
    public static final String CONFIRMED_PREFIX = "confirmed:";          // idempotent 처리용 키
    public static final String RESERVED_ORDER_PREFIX = "reserved:order:"; // orderId → [optionCode1, optionCode2, ...]

}