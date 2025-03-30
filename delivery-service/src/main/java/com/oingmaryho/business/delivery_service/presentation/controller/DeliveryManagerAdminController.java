package com.oingmaryho.business.delivery_service.presentation.controller;

import com.oingmaryho.business.common.domain.type.UserRoleType;
import com.oingmaryho.business.common.infrastructure.annotation.RequiredRoles;
import com.oingmaryho.business.delivery_service.application.dto.request.DeliveryManagerDetailRequestServiceDto;
import com.oingmaryho.business.delivery_service.application.dto.request.DeliveryManagerSearchRequestServiceDto;
import com.oingmaryho.business.delivery_service.application.dto.response.DeliveryManagerResponseServiceDto;
import com.oingmaryho.business.delivery_service.application.service.DeliveryAdminService;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryManagerType;
import com.oingmaryho.business.delivery_service.presentation.dto.mapper.DeliveryPresentationMapper;
import com.oingmaryho.business.delivery_service.presentation.dto.request.DeliveryManagerSearchRequestDto;
import com.oingmaryho.business.delivery_service.presentation.dto.response.DeliveryManagerResponseDto;
import com.oingmaryho.business.delivery_service.utils.PageableUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("admin/v1/deliveries/managers")
public class DeliveryManagerAdminController {

    private final DeliveryAdminService deliveryAdminService;
    private final DeliveryPresentationMapper deliveryPresentationMapper;

    @Description(
            "마스터 - 배송 담당자 상세 조회"
    )
    @RequiredRoles(UserRoleType.MASTER)
    @GetMapping("/{id}")
    public ResponseEntity<DeliveryManagerResponseDto> getDeliveryManagerDetail(
            HttpServletRequest request,
            @PathVariable UUID id
    ) {

        Long userId = (Long) request.getAttribute("userId");
        String userRole = (String) request.getAttribute("role");

        DeliveryManagerDetailRequestServiceDto requestServiceDto = deliveryPresentationMapper.toManagerDetailRequestDto(id);
        DeliveryManagerResponseServiceDto responseServiceDto = deliveryAdminService.GetDeliveryManagerDetail(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.ok(deliveryPresentationMapper.toMangerResponseDto(responseServiceDto));
    }


    @Description(
            "마스터 - 배송 담당자 검색"
    )
    @RequiredRoles(UserRoleType.MASTER)
    @PostMapping
    public ResponseEntity<Page<DeliveryManagerResponseDto>> searchDeliveryManager(
            HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "DESC") String sortDirection,
            @RequestParam(value = "by", required = false) String by,
            @RequestParam(value = "id", required = false) UUID id,
            @RequestParam(value = "slackId", required = false) String slackId,
            @RequestParam(value = "hubId", required = false) UUID hubId,
            @RequestParam(value = "managerId", required = false) Long managerId,
            @RequestParam(value = "type", required = false) DeliveryManagerType type,
            @RequestParam(value = "sequence", required = false) Integer sequence,
            @RequestParam(value = "isDeleted", required = false) Boolean isDeleted) {

        Long userId = (Long) request.getAttribute("userId");
        String userRole = (String) request.getAttribute("role");

        DeliveryManagerSearchRequestDto requestDto = new DeliveryManagerSearchRequestDto(
                id,
                slackId,
                hubId,
                managerId,
                type,
                sequence,
                isDeleted);


        Pageable customPageable = PageableUtils.customPageable(page, size, sortDirection, by);

        DeliveryManagerSearchRequestServiceDto requestServiceDto = deliveryPresentationMapper.toManagerSearchRequestDto(requestDto, customPageable);
        Page<DeliveryManagerResponseServiceDto> responseServiceDtos = deliveryAdminService.GetDeliveryManagerBySearch(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.ok(responseServiceDtos.map(deliveryPresentationMapper::toMangerResponseDto));
    }


}
