package com.oingmaryho.business.productservice.presentation.controller.command;

import com.oingmaryho.business.common.domain.type.UserRoleType;
import com.oingmaryho.business.common.infrastructure.annotation.RequiredRoles;
import com.oingmaryho.business.productservice.application.dto.request.ProductCreateRequestServiceDto;
import com.oingmaryho.business.productservice.application.dto.request.ProductDeleteRequestServiceDto;
import com.oingmaryho.business.productservice.application.dto.request.ProductDetailsSearchRequestServiceDto;
import com.oingmaryho.business.productservice.application.dto.request.ProductUpdateRequestServiceDto;
import com.oingmaryho.business.productservice.application.dto.response.ProductCreateResponseServiceDto;
import com.oingmaryho.business.productservice.application.dto.response.ProductDetailsSearchResponseServiceDto;
import com.oingmaryho.business.productservice.application.dto.response.ProductSearchResponseServiceDto;
import com.oingmaryho.business.productservice.application.dto.response.ProductUpdateResponseServiceDto;
import com.oingmaryho.business.productservice.application.service.ProductAdminService;
import com.oingmaryho.business.productservice.presentation.dto.request.ProductCreateRequestDto;
import com.oingmaryho.business.productservice.presentation.dto.request.ProductSearchRequestDto;
import com.oingmaryho.business.productservice.presentation.dto.request.ProductUpdateRequestDto;
import com.oingmaryho.business.productservice.presentation.dto.response.ProductCreateResponseDto;
import com.oingmaryho.business.productservice.presentation.dto.response.ProductDetailsSearchResponseDto;
import com.oingmaryho.business.productservice.presentation.dto.response.ProductSearchResponseDto;
import com.oingmaryho.business.productservice.presentation.dto.response.ProductUpdateResponseDto;
import com.oingmaryho.business.productservice.presentation.mapper.ProductPresentationMapper;
import com.oingmaryho.business.productservice.utils.PageableUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/products")
public class ProductAdminController {

    private final ProductAdminService productService;
    private final ProductPresentationMapper productPresentationMapper;

    @Description("마스터 - 상품 등록")
    @RequiredRoles(UserRoleType.MASTER)
    @PostMapping
    public ResponseEntity<ProductCreateResponseDto> createProduct(
        @RequestBody ProductCreateRequestDto productCreateRequestDto
    ) {

        ProductCreateRequestServiceDto requestServiceDto = productPresentationMapper.toCreateServiceDto(productCreateRequestDto);
        ProductCreateResponseServiceDto responseServiceDto = productService.createProduct(requestServiceDto);
        ProductCreateResponseDto responseDto = productPresentationMapper.toCreateDto(responseServiceDto);
        return ResponseEntity.ok(responseDto);
    }

    @Description("마스터 - 상품 전체 조회")
    @RequiredRoles(UserRoleType.MASTER)
    @GetMapping
    public ResponseEntity<Page<ProductSearchResponseDto>> getProducts(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size,
        @RequestParam(name = "sortDirection", defaultValue = "ASC") String sortDirection,
        @RequestParam(name = "by", defaultValue = "name") String by,
        @RequestParam(value = "id", required = false) UUID id,
        @RequestParam(name = "productCode", required = false) String productCode,
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "manageHubId", required = false) UUID manageHubId,
        @RequestParam(name = "companyId", required = false) UUID companyId,
        @RequestParam(name = "companyName", required = false) String companyName,
        @RequestParam(name = "minPrice", required = false) Integer minPrice,
        @RequestParam(name = "maxPrice", required = false) Integer maxPrice,
        @RequestParam(name = "minStock", required = false) Integer minStock,
        @RequestParam(name = "maxStock", required = false) Integer maxStock,
        @RequestParam(name = "isDeleted", required = false) Boolean isDeleted
    ) {

        Pageable pageable = PageableUtils.customPageable(page, size, sortDirection, by);
        ProductSearchRequestDto requestDto = new ProductSearchRequestDto(id, productCode, name, manageHubId, companyId, companyName, minPrice, maxPrice, minStock, maxStock, isDeleted);

        Page<ProductSearchResponseServiceDto> responseDto = productService.searchProducts(productPresentationMapper.toProductSearchRequestServiceDto(requestDto), pageable);

        return ResponseEntity.ok(responseDto.map(productPresentationMapper::toProductSearchResponseDto));
    }

    @Description("마스터 - 상품 상세 조회")
    @RequiredRoles(UserRoleType.MASTER)
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailsSearchResponseDto> getProductById(
        @PathVariable UUID id
    ) {

        ProductDetailsSearchRequestServiceDto requestServiceDto = productPresentationMapper.toDetailsSearchServiceDto(id);
        ProductDetailsSearchResponseServiceDto responseServiceDto = productService.getProductDetails(requestServiceDto);
        ProductDetailsSearchResponseDto response = productPresentationMapper.toDetailsSearchDto(responseServiceDto);
        return ResponseEntity.ok(response);
    }

    @Description("마스터 - 상품 수정")
    @RequiredRoles(UserRoleType.MASTER)
    @PutMapping("/{id}")
    public ResponseEntity<ProductUpdateResponseDto> updateProduct(
        @PathVariable UUID id,
        @RequestBody ProductUpdateRequestDto productUpdateRequestDto
    ) {

        ProductUpdateRequestServiceDto requestServiceDto = productPresentationMapper.toUpdateServiceDto(id, productUpdateRequestDto);
        ProductUpdateResponseServiceDto responseServiceDto = productService.updateProduct(requestServiceDto);
        ProductUpdateResponseDto responseDto = productPresentationMapper.toUpdateResponseDto(responseServiceDto);
        return ResponseEntity.ok(responseDto);
    }

    @Description("일반 상품 삭제")
    @RequiredRoles(UserRoleType.MASTER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
        @PathVariable UUID id,
        HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        ProductDeleteRequestServiceDto requestServiceDto = productPresentationMapper.toDeleteServiceDto(id);
        productService.deleteProduct(requestServiceDto, userId);
        return ResponseEntity.noContent().build();
    }
}
