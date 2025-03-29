package com.oingmaryho.business.delivery_service.presentation.controller;

import com.oingmaryho.business.common.domain.type.UserRoleType;
import com.oingmaryho.business.common.infrastructure.annotation.RequiredRoles;
import com.oingmaryho.business.delivery_service.application.service.DeliveryAdminService;
import com.oingmaryho.business.delivery_service.application.dto.request.*;
import com.oingmaryho.business.delivery_service.application.dto.response.*;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryRouteStatus;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryStatus;
import com.oingmaryho.business.delivery_service.utils.PageableUtils;
import com.oingmaryho.business.delivery_service.presentation.dto.mapper.DeliveryPresentationMapper;
import com.oingmaryho.business.delivery_service.presentation.dto.request.*;
import com.oingmaryho.business.delivery_service.presentation.dto.response.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/deliveries")
public class DeliveryAdminController {

    private final DeliveryAdminService deliveryAdminService;
    private final DeliveryPresentationMapper deliveryPresentationMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${message.queue.hubDeliveryManager}")
    private String queueHubDeliveryManager;

    @Description(
            "마스터 - 배송 생성"
    )
    @Deprecated
    @RequiredRoles(UserRoleType.MASTER)
    @PostMapping
    public ResponseEntity<UUID> createDelivery(
            @RequestBody DeliveryCreationRequestDto requestDto) {
        DeliveryManagerAssignmentRequestServiceDto requestServiceDto = deliveryAdminService.createDelivery(
                deliveryPresentationMapper.toCreationServiceDto(requestDto));

        rabbitTemplate.convertAndSend(queueHubDeliveryManager, new DeliveryManagerAssignmentRequestDto(
                requestServiceDto.deliveryId()
        ));

        return ResponseEntity.ok(requestServiceDto.deliveryId());
    }

    @Description(
            "마스터 - 배송 수정"
    )
    @RequiredRoles(UserRoleType.MASTER)
    @PutMapping("/{id}")
    public ResponseEntity<DeliveryUpdateResponseDto> updateDelivery(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody DeliveryUpdateRequestDto requestDto) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryUpdateRequestServiceDto requestServiceDto = deliveryPresentationMapper.toUpdateServiceDto(id, requestDto);
        DeliveryUpdateResponseServiceDto responseServiceDto = deliveryAdminService.updateDelivery(
                userId,
                userRole,
                requestServiceDto);
        return ResponseEntity.ok(deliveryPresentationMapper.toUpdateResponseDto(responseServiceDto));
    }

    @Description(
            "마스터 - 배송 상태 수정"
    )
    @RequiredRoles(UserRoleType.MASTER)
    @PutMapping("/{id}/status")
    public ResponseEntity<DeliveryUpdateStatusResponseDto> updateDeliveryStatus(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody DeliveryUpdateStatusRequestDto requestDto) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryUpdateStatusRequestServiceDto requestServiceDto = deliveryPresentationMapper.toUpdateStatusServiceDto(id, requestDto);
        DeliveryUpdateStatusResponseServiceDto responseServiceDto = deliveryAdminService.updateStatusDelivery(
                userId,
                userRole,
                requestServiceDto);
        return ResponseEntity.ok(deliveryPresentationMapper.toUpdateStatusResponseDto(responseServiceDto));
    }

    @Description(
            "마스터 - 배송 삭제"
    )
    @RequiredRoles(UserRoleType.MASTER)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelivery(
            HttpServletRequest request,
            @PathVariable UUID id) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryDeletionRequestServiceDto requestServiceDto = deliveryPresentationMapper.toDeletionServiceDto(id);
        deliveryAdminService.deleteDelivery(
                userId,
                userRole,
                requestServiceDto);
        return ResponseEntity.noContent().build();
    }

    @Description(
            "마스터 - 배송 상세 조회"
    )
    @RequiredRoles(UserRoleType.MASTER)
    @GetMapping("/{id}")
    public ResponseEntity<DeliveryAdminResponseDto> getDeliveryDetail(
            HttpServletRequest request,
            @PathVariable UUID id) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryDetailRequestServiceDto requestServiceDto = deliveryPresentationMapper.toDetailServiceDto(id);
        DeliveryResponseServiceDto responseServiceDto = deliveryAdminService.GetDeliveryDetail(
                userId,
                userRole,
                requestServiceDto);
        return ResponseEntity.ok(deliveryPresentationMapper.toDetailAdminResponseDto(responseServiceDto));
    }

    @Description(
            "마스터 - 배송 전체 조회(검색)"
    )
    @RequiredRoles(UserRoleType.MASTER)
    @GetMapping
    public ResponseEntity<Page<DeliveryAdminResponseDto>> searchDelivery(
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
            @RequestParam(value = "managerId", required = false) Long managerId,
            @RequestParam(value = "isDeleted", required = false) Boolean isDeleted) {

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
                isDeleted);

        Pageable customPageable = PageableUtils.customPageable(page, size, sortDirection, by);

        DeliverySearchRequestServiceDto requestServiceDto = deliveryPresentationMapper.toSearchServiceDto(requestDto, customPageable);
        Page<DeliveryResponseServiceDto> responseServiceDtos = deliveryAdminService.GetDeliveriesBySearch(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.ok(responseServiceDtos.map(deliveryPresentationMapper::toSearchAdminResponseDto));
    }

    @Description(
            "마스터 - 배송 경로 조회"
    )
    @RequiredRoles(UserRoleType.MASTER)
    @GetMapping("/routes/{id}")
    public ResponseEntity<DeliveryRouteAdminResponseDto> getDeliveryRouteDetail(
            HttpServletRequest request,
            @PathVariable UUID id) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryRouteDetailRequestServiceDto requestServiceDto = deliveryPresentationMapper.toRouteDetailServiceDto(id);
        DeliveryRouteResponseServiceDto responseServiceDto = deliveryAdminService.GetDeliveryRouteDetail(
                userId,
                userRole,
                requestServiceDto);
        return ResponseEntity.ok(deliveryPresentationMapper.toRouteDetailAdminResponseDto(responseServiceDto));
    }

    @Description(
            "마스터 - 배송 경로 전체 조회(검색)"
    )
    @RequiredRoles(UserRoleType.MASTER)
    @GetMapping("/{id}/routes")
    public ResponseEntity<Page<DeliveryRouteAdminResponseDto>> searchDeliveryRoute(
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
        Page<DeliveryRouteResponseServiceDto> responseServiceDtos = deliveryAdminService.GetDeliveryRoutesBySearch(
                userId,
                userRole,
                requestServiceDto);

        return ResponseEntity.ok(responseServiceDtos.map(deliveryPresentationMapper::toRouteSearchAdminResponseDto));
    }

    @Description(
            "마스터 - 배송 경로 상태 수정"
    )
    @RequiredRoles(UserRoleType.MASTER)
    @PutMapping("/routes/{id}/status")
    public ResponseEntity<DeliveryRouteUpdateStatusResponseDto> updateDeliveryRouteStatus(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody DeliveryRouteUpdateStatusRequestDto requestDto) {

        Long userId = (Long) request.getAttribute("userId");
        String userRoleStr = (String) request.getAttribute("role");
        UserRoleType userRole = UserRoleType.valueOf(userRoleStr);

        DeliveryRouteUpdateStatusRequestServiceDto requestServiceDto = deliveryPresentationMapper.toUpdateRouteStatusServiceDto(id, requestDto);
        DeliveryRouteUpdateStatusResponseServiceDto responseServiceDto = deliveryAdminService.updateRouteStatusDelivery(
                userId,
                userRole,
                requestServiceDto);
        return ResponseEntity.ok(deliveryPresentationMapper.toUpdateRouteStatusResponseDto(responseServiceDto));
    }



}
