package com.oingmaryho.business.productservice.domain.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReadModel {

    private UUID productId;
    private String name;
    private int basePrice;
    private List<ProductOption> options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductOption {
        private String optionCode;
        private String optionName;
        private int extraPrice;
        private int quantity;
    }
}

