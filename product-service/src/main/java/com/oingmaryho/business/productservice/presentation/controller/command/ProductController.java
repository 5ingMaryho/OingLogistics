package com.oingmaryho.business.productservice.presentation.controller.command;

import java.util.UUID;

import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oingmaryho.business.common.domain.type.UserRoleType;
import com.oingmaryho.business.common.infrastructure.annotation.RequiredRoles;
import com.oingmaryho.business.productservice.application.dto.request.ProductDetailsSearchRequestServiceDto;
import com.oingmaryho.business.productservice.application.dto.request.ProductUpdateRequestServiceDto;
import com.oingmaryho.business.productservice.application.dto.response.ProductDetailsSearchResponseServiceDto;
import com.oingmaryho.business.productservice.application.dto.response.ProductSearchResponseServiceDto;
import com.oingmaryho.business.productservice.application.dto.response.ProductUpdateResponseServiceDto;
import com.oingmaryho.business.productservice.application.service.ProductService;
import com.oingmaryho.business.productservice.application.dto.request.ProductCreateRequestServiceDto;
import com.oingmaryho.business.productservice.application.dto.response.ProductCreateResponseServiceDto;
import com.oingmaryho.business.productservice.presentation.dto.request.ProductCreateRequestDto;
import com.oingmaryho.business.productservice.presentation.dto.request.ProductSearchRequestDto;
import com.oingmaryho.business.productservice.presentation.dto.request.ProductUpdateRequestDto;
import com.oingmaryho.business.productservice.presentation.dto.response.ProductCreateResponseDto;
import com.oingmaryho.business.productservice.application.dto.request.ProductDeleteRequestServiceDto;
import com.oingmaryho.business.productservice.presentation.dto.response.ProductDetailsSearchResponseDto;
import com.oingmaryho.business.productservice.presentation.dto.response.ProductSearchResponseDto;
import com.oingmaryho.business.productservice.presentation.dto.response.ProductUpdateResponseDto;
import com.oingmaryho.business.productservice.presentation.mapper.ProductPresentationMapper;
import com.oingmaryho.business.productservice.utils.PageableUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final ProductPresentationMapper productPresentationMapper;

    @Description("일반 - 상품 등록")
    @RequiredRoles({UserRoleType.HUB_MANAGER, UserRoleType.COMPANY_MANAGER})
    @PostMapping
    public ResponseEntity<ProductCreateResponseDto> createProduct(
        @RequestBody ProductCreateRequestDto productCreateRequestDto,
        HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        String roleStr = (String) request.getAttribute("role");
        UserRoleType role = UserRoleType.valueOf(roleStr);

        ProductCreateRequestServiceDto requestServiceDto = productPresentationMapper.toCreateServiceDto(productCreateRequestDto);
        ProductCreateResponseServiceDto responseServiceDto = productService.createProduct(requestServiceDto, userId, role);
        ProductCreateResponseDto responseDto = productPresentationMapper.toCreateDto(responseServiceDto);
        return ResponseEntity.ok(responseDto);
    }

    @Description("일반 - 상품 전체 조회")
    @RequiredRoles({UserRoleType.HUB_MANAGER, UserRoleType.COMPANY_MANAGER, UserRoleType.COMPANY_DELIVERY_MANAGER, UserRoleType.HUB_DELIVERY_MANAGER})
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
        HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        String roleStr = (String) request.getAttribute("role");
        UserRoleType role = UserRoleType.valueOf(roleStr);

        Pageable pageable = PageableUtils.customPageable(page, size, sortDirection, by);
        Boolean isDeleted = false;
        ProductSearchRequestDto requestDto = new ProductSearchRequestDto(id, productCode, name, manageHubId, companyId, companyName, minPrice, maxPrice, minStock, maxStock, isDeleted);

        Page<ProductSearchResponseServiceDto> responseDto = productService.searchProducts(productPresentationMapper.toProductSearchRequestServiceDto(requestDto), pageable, userId, role);

        return ResponseEntity.ok(responseDto.map(productPresentationMapper::toProductSearchResponseDto));
    }

    @Description("일반 - 상품 상세 조회")
    @RequiredRoles({UserRoleType.HUB_MANAGER, UserRoleType.COMPANY_MANAGER, UserRoleType.COMPANY_DELIVERY_MANAGER, UserRoleType.HUB_DELIVERY_MANAGER})
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailsSearchResponseDto> getProductById(
        @PathVariable UUID id,
        HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        String roleStr = (String) request.getAttribute("role");
        UserRoleType role = UserRoleType.valueOf(roleStr);

        ProductDetailsSearchRequestServiceDto requestServiceDto = productPresentationMapper.toDetailsSearchServiceDto(id);
        ProductDetailsSearchResponseServiceDto responseServiceDto = productService.getProductDetails(requestServiceDto, userId, role);
        ProductDetailsSearchResponseDto response = productPresentationMapper.toDetailsSearchDto(responseServiceDto);
        return ResponseEntity.ok(response);
    }

    @Description("일반 - 상품 수정")
    @RequiredRoles({UserRoleType.HUB_MANAGER, UserRoleType.COMPANY_MANAGER})
    @PutMapping("/{id}")
    public ResponseEntity<ProductUpdateResponseDto> updateProduct(
        @PathVariable UUID id,
        @RequestBody ProductUpdateRequestDto productUpdateRequestDto,
        HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        String roleStr = (String) request.getAttribute("role");
        UserRoleType role = UserRoleType.valueOf(roleStr);
        ProductUpdateRequestServiceDto requestServiceDto = productPresentationMapper.toUpdateServiceDto(id, productUpdateRequestDto);
        ProductUpdateResponseServiceDto responseServiceDto = productService.updateProduct(requestServiceDto, userId, role);
        ProductUpdateResponseDto responseDto = productPresentationMapper.toUpdateResponseDto(responseServiceDto);
        return ResponseEntity.ok(responseDto);
    }

    @Description("일반 상품 삭제")
    @RequiredRoles({UserRoleType.HUB_MANAGER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
        @PathVariable UUID id,
        HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        String roleStr = (String) request.getAttribute("role");
        UserRoleType role = UserRoleType.valueOf(roleStr);
        ProductDeleteRequestServiceDto requestServiceDto = productPresentationMapper.toDeleteServiceDto(id);
        productService.deleteProduct(requestServiceDto, userId, role);
        return ResponseEntity.noContent().build();
    }
}
