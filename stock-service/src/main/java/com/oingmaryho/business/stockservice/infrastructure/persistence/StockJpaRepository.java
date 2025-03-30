package com.oingmaryho.business.stockservice.infrastructure.persistence;

import com.oingmaryho.business.stockservice.domain.model.Stock;
import com.oingmaryho.business.stockservice.domain.repository.StockRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockJpaRepository extends JpaRepository<Stock, UUID>, StockRepository {
}
