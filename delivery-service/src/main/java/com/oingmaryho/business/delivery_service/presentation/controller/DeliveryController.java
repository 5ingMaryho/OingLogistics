package com.oingmaryho.business.delivery_service.presentation.controller;

import com.oingmaryho.business.common.domain.type.UserRoleType;
import com.oingmaryho.business.common.infrastructure.annotation.RequiredRoles;
import com.oingmaryho.business.delivery_service.application.service.DeliveryService;
import com.oingmaryho.business.delivery_service.application.dto.request.*;
import com.oingmaryho.business.delivery_service.application.dto.response.*;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryRouteStatus;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryStatus;
import com.oingmaryho.business.delivery_service.presentation.dto.request.*;
import com.oingmaryho.business.delivery_service.utils.PageableUtils;
import com.oingmaryho.business.delivery_service.presentation.dto.mapper.DeliveryPresentationMapper;
import com.oingmaryho.business.delivery_service.presentation.dto.response.*;
import com.oingmaryho.business.delivery_service.presentation.dto.response.DeliveryRouteResponseDto;

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
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final DeliveryPresentationMapper deliveryPresentationMapper;

    @Description(
            "허브 관리자 - 배송 수정"
    )
    @RequiredRoles(UserRoleType.HUB_MANAGER)
    @PutMapping("/{id}")
    public ResponseEntity<DeliveryUpdateResponseDto> updateDelivery(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody DeliveryUpdateRequestDto requestDto) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryUpdateRequestServiceDto requestServiceDto = deliveryPresentationMapper.toUpdateServiceDto(id, requestDto);
        DeliveryUpdateResponseServiceDto responseServiceDto = deliveryService.updateDelivery(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.ok(deliveryPresentationMapper.toUpdateResponseDto(responseServiceDto));
    }

    @Description(
            "허브 관리자, 허브 배송 담당자, 업체 배송 담당자 - 배송 수정"
    )
    @RequiredRoles({
            UserRoleType.HUB_MANAGER,
            UserRoleType.HUB_DELIVERY_MANAGER,
            UserRoleType.COMPANY_DELIVERY_MANAGER
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<DeliveryUpdateStatusResponseDto> updateDeliveryStatus(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody DeliveryUpdateStatusRequestDto requestDto) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryUpdateStatusRequestServiceDto requestServiceDto = deliveryPresentationMapper.toUpdateStatusServiceDto(id, requestDto);
        DeliveryUpdateStatusResponseServiceDto responseServiceDto = deliveryService.updateStatusDelivery(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.ok(deliveryPresentationMapper.toUpdateStatusResponseDto(responseServiceDto));
    }

    @Description(
            "허브 관리자 - 배송 삭제"
    )
    @RequiredRoles(UserRoleType.HUB_MANAGER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelivery(
            HttpServletRequest request,
            @PathVariable UUID id) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryDeletionRequestServiceDto requestServiceDto = deliveryPresentationMapper.toDeletionServiceDto(id);
        deliveryService.deleteDelivery(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.noContent().build();
    }


    @Description(
            "허브 관리자, 허브 배송 담당자, 업체 배송 담당자, 업체 담당자 - 배송 상세 조회"
    )
    @RequiredRoles({
            UserRoleType.HUB_MANAGER,
            UserRoleType.HUB_DELIVERY_MANAGER,
            UserRoleType.COMPANY_DELIVERY_MANAGER,
            UserRoleType.COMPANY_MANAGER
    })
    @GetMapping("/{id}")
    public ResponseEntity<DeliveryResponseDto> getDeliveryDetail(
            HttpServletRequest request,
            @PathVariable UUID id) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryDetailRequestServiceDto requestServiceDto = deliveryPresentationMapper.toDetailServiceDto(id);
        DeliveryResponseServiceDto responseServiceDto = deliveryService.GetDeliveryDetail(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.ok(deliveryPresentationMapper.toDetailResponseDto(responseServiceDto));
    }

    @Description(
            "허브 관리자, 허브 배송 담당자, 업체 배송 담당자, 업체 담당자 - 배송 전체 조회(검색)"
    )
    @RequiredRoles({
            UserRoleType.HUB_MANAGER,
            UserRoleType.HUB_DELIVERY_MANAGER,
            UserRoleType.COMPANY_DELIVERY_MANAGER,
            UserRoleType.COMPANY_MANAGER
    })
    @GetMapping
    public ResponseEntity<Page<DeliveryResponseDto>> searchDelivery(
            HttpServletRequest request,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "DESC") String sortDirection,
            @RequestParam(value = "by", required = false) String by,
            @RequestParam(value = "id", required = false) UUID id,
            @RequestParam(value = "orderId", required = false) UUID orderId,
            @RequestParam(value = "orderDetailId", required = false) UUID orderDetailId,
            @RequestParam(value = "hubId", required = false) UUID hubId,
            @RequestParam(value = "companyId", required = false) UUID companyId,
            @RequestParam(value = "status", required = false) DeliveryStatus status,
            @RequestParam(value = "managerId", required = false) Long managerId) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliverySearchRequestDto requestDto = new DeliverySearchRequestDto(
                id,
                orderId,
                orderDetailId,
                hubId,
                companyId,
                status,
                managerId,
                Boolean.FALSE);

        Pageable customPageable = PageableUtils.customPageable(page, size, sortDirection, by);

        DeliverySearchRequestServiceDto requestServiceDto = deliveryPresentationMapper.toSearchServiceDto(requestDto, customPageable);
        Page<DeliveryResponseServiceDto> responseServiceDtos = deliveryService.GetDeliveriesBySearch(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.ok(responseServiceDtos.map(deliveryPresentationMapper::toSearchResponseDto));
    }

    @Description(
            "허브 관리자, 허브 배송 담당자, 업체 배송 담당자, 업체 담당자 - 배송 경로 상세 조회"
    )
    @RequiredRoles({
            UserRoleType.HUB_MANAGER,
            UserRoleType.HUB_DELIVERY_MANAGER,
            UserRoleType.COMPANY_DELIVERY_MANAGER,
            UserRoleType.COMPANY_MANAGER
    })
    @GetMapping("/routes/{id}")
    public ResponseEntity<DeliveryRouteResponseDto> getDeliveryRouteDetail(
            HttpServletRequest request,
            @PathVariable UUID id) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryRouteDetailRequestServiceDto requestServiceDto = deliveryPresentationMapper.toRouteDetailServiceDto(id);
        DeliveryRouteResponseServiceDto responseServiceDto = deliveryService.GetDeliveryRouteDetail(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.ok(deliveryPresentationMapper.toRouteDetailResponseDto(responseServiceDto));
    }

    @Description(
            "허브 관리자, 허브 배송 담당자, 업체 배송 담당자, 업체 담당자 - 배송 경로 전체 조회(검색)"
    )
    @RequiredRoles({
            UserRoleType.HUB_MANAGER,
            UserRoleType.HUB_DELIVERY_MANAGER,
            UserRoleType.COMPANY_DELIVERY_MANAGER,
            UserRoleType.COMPANY_MANAGER
    })
    @GetMapping("/{id}/routes")
    public ResponseEntity<Page<DeliveryRouteResponseDto>> searchDeliveryRoute(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "DESC") String sortDirection,
            @RequestParam(value = "by", required = false) String by,
            @RequestParam(value = "routeId", required = false) UUID routeId,
            @RequestParam(value = "orderId", required = false) UUID orderId,
            @RequestParam(value = "orderDetailId", required = false) UUID orderDetailId,
            @RequestParam(value = "departureHubId", required = false) UUID departureHubId,
            @RequestParam(value = "arriveHubId", required = false) UUID arriveHubId,
            @RequestParam(value = "companyId", required = false) UUID companyId,
            @RequestParam(value = "managerId", required = false) Long managerId,
            @RequestParam(value = "status", required = false) DeliveryRouteStatus status) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryRouteSearchRequestDto requestDto = new DeliveryRouteSearchRequestDto(
                routeId,
                orderId,
                orderDetailId,
                departureHubId,
                arriveHubId,
                companyId,
                managerId,
                status,
                Boolean.FALSE);

        Pageable customPageable = PageableUtils.customPageable(page, size, sortDirection, by);
        DeliveryRouteSearchRequestServiceDto requestServiceDto = deliveryPresentationMapper.toRouteSearchServiceDto(id, requestDto, customPageable);
        Page<DeliveryRouteResponseServiceDto> responseServiceDtos = deliveryService.GetDeliveryRoutesBySearch(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.ok(responseServiceDtos.map(deliveryPresentationMapper::toRouteSearchResponseDto));
    }

    @Description(
            "허브 관리자, 허브 배송 담당자, 업체 배송 담당자 - 배송 경로 상태 수정"
    )
    @RequiredRoles({
            UserRoleType.HUB_MANAGER,
            UserRoleType.HUB_DELIVERY_MANAGER,
            UserRoleType.COMPANY_DELIVERY_MANAGER
    })
    @PutMapping("/routes/{id}/status")
    public ResponseEntity<DeliveryRouteUpdateStatusResponseDto> updateDeliveryRouteStatus(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody DeliveryRouteUpdateStatusRequestDto requestDto) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryRouteUpdateStatusRequestServiceDto requestServiceDto = deliveryPresentationMapper.toUpdateRouteStatusServiceDto(id, requestDto);
        DeliveryRouteUpdateStatusResponseServiceDto responseServiceDto =
                deliveryService.updateRouteStatusDelivery(
                        userId,
                        userRole,
                        requestServiceDto);

        return ResponseEntity.ok(deliveryPresentationMapper.toUpdateRouteStatusResponseDto(responseServiceDto));
    }


}
