package com.oingmaryho.business.productservice.presentation.controller.command;

import java.util.UUID;

import org.springframework.context.annotation.Description;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oingmaryho.business.productservice.application.service.ProductFeignClientService;
import com.oingmaryho.business.productservice.application.dto.request.ProductDetailsSearchRequestServiceDto;
import com.oingmaryho.business.productservice.application.dto.response.ProductDetailsSearchResponseServiceDto;
import com.oingmaryho.business.productservice.presentation.dto.response.ProductDetailsSearchResponseDto;
import com.oingmaryho.business.productservice.presentation.mapper.ProductPresentationMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product-service/products")
public class ProductFeignClientController {
    private final ProductFeignClientService productService;
    private final ProductPresentationMapper productPresentationMapper;

    @Description("FeignClient - 상품 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailsSearchResponseDto> getProductById(@PathVariable UUID id) {
        ProductDetailsSearchRequestServiceDto requestServiceDto = productPresentationMapper.toDetailsSearchServiceDto(id);
        ProductDetailsSearchResponseServiceDto responseServiceDto = productService.getProduct(requestServiceDto);
        ProductDetailsSearchResponseDto response = productPresentationMapper.toDetailsSearchDto(responseServiceDto);
        return ResponseEntity.ok(response);
    }
}
