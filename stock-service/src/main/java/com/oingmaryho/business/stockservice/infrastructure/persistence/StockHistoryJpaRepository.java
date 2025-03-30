package com.oingmaryho.business.stockservice.infrastructure.persistence;

import com.oingmaryho.business.stockservice.domain.model.StockHistory;
import com.oingmaryho.business.stockservice.domain.repository.StockHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockHistoryJpaRepository extends JpaRepository<StockHistory, UUID>, StockHistoryRepository {
}
