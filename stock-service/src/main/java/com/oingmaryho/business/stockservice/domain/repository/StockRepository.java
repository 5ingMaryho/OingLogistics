package com.oingmaryho.business.stockservice.domain.repository;

import com.oingmaryho.business.stockservice.domain.model.Stock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository {
    Optional<Stock> findByOptionCode(String optionCode);

    Stock save(Stock stock);
}
