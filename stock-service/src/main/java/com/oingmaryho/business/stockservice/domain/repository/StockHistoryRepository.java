package com.oingmaryho.business.stockservice.domain.repository;

import com.oingmaryho.business.stockservice.domain.model.StockHistory;

public interface StockHistoryRepository {
    StockHistory save(StockHistory history);
}
