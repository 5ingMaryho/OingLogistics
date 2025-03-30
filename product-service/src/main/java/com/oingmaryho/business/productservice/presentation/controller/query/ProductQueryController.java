package com.oingmaryho.business.productservice.presentation.controller.query;

import com.oingmaryho.business.productservice.domain.model.ProductReadModel;
import com.oingmaryho.business.productservice.infrastructure.redis.RedisReadModelRepository;
import com.oingmaryho.business.productservice.presentation.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductQueryController {

    private final RedisReadModelRepository redisRepo;

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable UUID productId) {
        ProductReadModel model = redisRepo.findByProductId(productId);
        if (model == null) {
            throw new IllegalArgumentException("상품이 존재하지 않습니다.");
        }

        return ProductResponse.builder()
            .productId(model.getProductId())
            .name(model.getName())
            .basePrice(model.getBasePrice())
            .options(model.getOptions().stream().map(option ->
                ProductResponse.ProductOption.builder()
                    .optionCode(option.getOptionCode())
                    .optionName(option.getOptionName())
                    .extraPrice(option.getExtraPrice())
                    .quantity(option.getQuantity())
                    .build()
            ).collect(Collectors.toList()))
            .build();
    }

    @GetMapping
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        List<ProductReadModel> allModels = redisRepo.findAll();

        // 페이징 계산
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allModels.size());

        List<ProductResponse> content = allModels.subList(start, end).stream()
            .map(model -> ProductResponse.builder()
                .productId(model.getProductId())
                .name(model.getName())
                .basePrice(model.getBasePrice())
                .options(model.getOptions().stream().map(option ->
                    ProductResponse.ProductOption.builder()
                        .optionCode(option.getOptionCode())
                        .optionName(option.getOptionName())
                        .extraPrice(option.getExtraPrice())
                        .quantity(option.getQuantity())
                        .build()
                ).collect(Collectors.toList()))
                .build())
            .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, allModels.size());
    }

}
