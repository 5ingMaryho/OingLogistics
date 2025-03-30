package com.oingmaryho.business.productservice.presentation.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {
    private UUID productId;
    private String name;
    private int basePrice;
    private List<ProductOption> options;

    @Data
    @Builder
    public static class ProductOption {
        private String optionCode;
        private String optionName;
        private int extraPrice;
        private int quantity;
    }
}
