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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_stock_history")
public class StockHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "option_code", nullable = false, length = 50)
    private String optionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ChangeType changeType;

    @Column(name = "change_quantity", nullable = false)
    private Integer changeQuantity;

    @Column(name = "reason", nullable = false, length = 50)
    private String reason;

    public enum ChangeType {
        INCREASE,
        DECREASE
    }
}
