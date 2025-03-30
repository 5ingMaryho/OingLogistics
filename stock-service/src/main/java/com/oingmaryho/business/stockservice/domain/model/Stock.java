package com.oingmaryho.business.stockservice.domain.model;

import com.oingmaryho.business.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.UUID;

@Getter
@Entity
@SuperBuilder
@DynamicInsert
@DynamicUpdate
@Table(name = "p_stock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "option_code", nullable = false, length = 50, unique = true)
    private String optionCode;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    public void decrease(int quantity) {
        if (this.stock < quantity) {
            // TODO: StockException 추가
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
        this.stock -= quantity;
    }

    public void increase(int quantity) {
        this.stock += quantity;
    }
}
