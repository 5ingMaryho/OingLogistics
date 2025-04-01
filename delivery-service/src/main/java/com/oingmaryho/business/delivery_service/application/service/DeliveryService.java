package com.oingmaryho.business.delivery_service.application.service;

import com.oingmaryho.business.common.domain.type.UserRoleType;
import com.oingmaryho.business.delivery_service.application.dto.mapper.DeliveryApplicationMapper;
import com.oingmaryho.business.delivery_service.application.dto.request.*;
import com.oingmaryho.business.delivery_service.application.dto.response.*;
import com.oingmaryho.business.delivery_service.application.feign.*;
import com.oingmaryho.business.delivery_service.domain.criteria.DeliveryManagerSearchCriteria;
import com.oingmaryho.business.delivery_service.domain.criteria.DeliveryRouteSearchCriteria;
import com.oingmaryho.business.delivery_service.domain.criteria.DeliverySearchCriteria;
import com.oingmaryho.business.delivery_service.domain.entity.Delivery;
import com.oingmaryho.business.delivery_service.domain.entity.DeliveryManager;
import com.oingmaryho.business.delivery_service.domain.entity.DeliveryRoute;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryManagerType;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryRouteStatus;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryStatus;
import com.oingmaryho.business.delivery_service.exception.DeliveryException;
import com.oingmaryho.business.delivery_service.exception.ErrorCode;
import com.oingmaryho.business.delivery_service.domain.repository.DeliveryManagerRepository;
import com.oingmaryho.business.delivery_service.domain.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final CompanyClient companyClient;
    private final HubClient hubClient;
    private final UserClient userClient;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryManagerRepository deliveryManagerRepository;
    private final DeliveryApplicationMapper deliveryApplicationMapper;


    /**
     * 배송 수정 - 허브 관리자
     * @param userId 사용자 id
     * @param userRole 사용자 권한
     * @param requestServiceDto 배송 수정 request
     * @return 배송 수정 response
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "delivery", key = "#requestServiceDto.id()"),
            @CacheEvict(cacheNames = "deliveries", allEntries = true)
    })
    public DeliveryUpdateResponseServiceDto updateDelivery(Long userId,
                                                           UserRoleType userRole,
                                                           DeliveryUpdateRequestServiceDto requestServiceDto) {
        Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

        // 허브 관리자 : 본인이 담당하는 허브의 배송인 지 유효성 검사
        if (userRole.equals(UserRoleType.HUB_MANAGER)) {

            HubSearchResponseDto hubDto = Optional.ofNullable(hubClient.getHubByManagerId(userId).getBody())
                    .orElseThrow(() -> new DeliveryException(ErrorCode.HUB_NOT_FOUND));

            UUID hubId = hubDto.id();
            // 허브 관리자의 담당 허브와 배송 출발 허브가 다를 경우
            if (delivery.getDepartureHubId() != hubId) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // managerId로 user 쪽에 수정하려는 manager가 '업체 배송 담당자'인지 유효성 검사
        UserRoleType userRoleType = Optional.ofNullable(
                userClient.getUserRoleById(requestServiceDto.managerId()).getBody()
        ).orElseThrow(() -> new DeliveryException(ErrorCode.USER_ROLE_NOT_FOUND));


        if (!userRoleType.equals(UserRoleType.COMPANY_DELIVERY_MANAGER)) {
            throw new DeliveryException(ErrorCode.BAD_REQUEST);
        }

        // managerId로 DeliveryManager 쪽에 수정하려는 manager가 '업체 배송 담당자'인지 유효성 검사
        DeliveryManager newManager = deliveryManagerRepository.findByManagerIdAndIsDeletedFalse(requestServiceDto.managerId())
                .orElseThrow(() -> new DeliveryException(ErrorCode.MANAGER_NOT_FOUND));
        if (!newManager.getType().equals(DeliveryManagerType.COMPANY_DELIVERY_MANAGER)) {
            throw new DeliveryException(ErrorCode.BAD_REQUEST);
        }

        // 수정하려는 manager가 배송의 마지막 허브에 속한 배송 담당자가 아닌 경우
        if (!newManager.getHubId().equals(delivery.getArriveHubId())) {
            throw new DeliveryException(ErrorCode.BAD_REQUEST);
        }

        delivery.update(requestServiceDto.receiver(), requestServiceDto.receiverSlackId(), requestServiceDto.address(), newManager);
        return deliveryApplicationMapper.toUpdateResponseServiceDto(delivery.getId());

    }

    /**
     * 배송 상태 수정 - 허브 관리자, 허브 배송 담당자, 업체 배송 담당자
     * @param userId 사용자 id
     * @param userRole 사용자 권한
     * @param requestServiceDto 배송 상태 수정 request
     * @return 배송 상태 수정 response
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "delivery", key = "#requestServiceDto.id()"),
            @CacheEvict(cacheNames = "deliveries", allEntries = true)
    })
    public DeliveryUpdateStatusResponseServiceDto updateStatusDelivery(Long userId,
                                                                       UserRoleType userRole,
                                                                       DeliveryUpdateStatusRequestServiceDto requestServiceDto) {
        Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

        // 허브 관리자 : 본인이 담당하는 허브의 배송인 지 유효성 검사
        if (userRole == UserRoleType.HUB_MANAGER) {
            HubSearchResponseDto hubDto = Optional.ofNullable(hubClient.getHubByManagerId(userId).getBody())
                    .orElseThrow(() -> new DeliveryException(ErrorCode.HUB_NOT_FOUND));

            UUID hubId = hubDto.id();
            // 허브 관리자의 담당 허브와 배송 출발 허브가 다를 경우
            if (delivery.getDepartureHubId() != hubId) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 업체 배송 담당자 : 본인이 담당하는 배송인 지 유효성 검사
        if (userRole == UserRoleType.COMPANY_DELIVERY_MANAGER) {
            // 담당하는 배송이 아닌 경우
            if (!delivery.getManager().getManagerId().equals(userId)) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 허브 배송 담당자 : 본인이 담당하는 배송인 지 유효성 검사
        if (userRole == UserRoleType.HUB_DELIVERY_MANAGER) {
            boolean flag = delivery.getRoutes().stream()
                    .anyMatch(route -> route.getManager().getManagerId().equals(userId));
            // 담당하는 배송이 아닌 경우
            if (!flag) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 배송 상태와 변경하려는 상태가 같은 경우
        if (delivery.getStatus().equals(requestServiceDto.status())) {
            throw new DeliveryException(ErrorCode.BAD_REQUEST);
        }

        delivery.updateStatus(requestServiceDto.status());
        return deliveryApplicationMapper.toUpdateStatusResponseServiceDto(delivery.getId());
    }


    /**
     * 배송 삭제 - 허브 관리자
     * @param userId 사용자 id
     * @param userRole 사용자 권한
     * @param requestServiceDto 삭제 request
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "delivery", key = "#requestServiceDto.id()"),
            @CacheEvict(cacheNames = "deliveries", allEntries = true)
    })
    public void deleteDelivery(Long userId,
                               UserRoleType userRole,
                               DeliveryDeletionRequestServiceDto requestServiceDto) {
        Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

        // 허브 관리자 : 본인이 담당하는 허브의 배송인 지 유효성 검사
        if (userRole == UserRoleType.HUB_MANAGER) {
            HubSearchResponseDto hubDto = Optional.ofNullable(hubClient.getHubByManagerId(userId).getBody())
                    .orElseThrow(() -> new DeliveryException(ErrorCode.HUB_NOT_FOUND));

            UUID hubId = hubDto.id();
            // 허브 관리자의 담당 허브와 배송 출발 허브가 다를 경우
            if (delivery.getDepartureHubId() != hubId) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        delivery.softDelete(userId);
    }

    /**
     * 배송 상세 조회 - 허브 관리자, 허브 배송 담당자, 업체 배송 담당자, 업체 담당자
     * @param userId 사용자 id
     * @param userRole 사용자 권한
     * @param requestServiceDto 배송 상세 조회 request
     * @return 배송 상세 조회 response
     */
    @Transactional(readOnly =true)
    @Cacheable(cacheNames = "delivery", key = "#requestServiceDto.id()")
    public DeliveryResponseServiceDto GetDeliveryDetail(Long userId,
                                                        UserRoleType userRole,
                                                        DeliveryDetailRequestServiceDto requestServiceDto) {
        Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

        // 허브 관리자 : 본인이 담당하는 허브의 배송인 지 유효성 검사
        if (userRole == UserRoleType.HUB_MANAGER) {
            HubSearchResponseDto hubDto = Optional.ofNullable(hubClient.getHubByManagerId(userId).getBody())
                    .orElseThrow(() -> new DeliveryException(ErrorCode.HUB_NOT_FOUND));

            UUID hubId = hubDto.id();
            // 허브 관리자의 담당 허브와 배송 출발 허브가 다를 경우
            if (delivery.getDepartureHubId() != hubId) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 업체 배송 담당자 : 본인이 담당하는 배송인 지 유효성 검사
        if (userRole == UserRoleType.COMPANY_DELIVERY_MANAGER) {
            // 담당하는 배송이 아닌 경우
            if (!delivery.getManager().getManagerId().equals(userId)) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 허브 배송 담당자 : 본인이 담당하는 배송인 지 유효성 검사
        if (userRole == UserRoleType.HUB_DELIVERY_MANAGER) {
            boolean flag = delivery.getRoutes().stream()
                    .anyMatch(route -> route.getManager().getManagerId().equals(userId));
            // 담당하는 배송이 아닌 경우
            if (!flag) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 업체 담당자 : 본인이 담당하는 업체의 배송인 지 유효성 검사
        if (userRole == UserRoleType.COMPANY_MANAGER) {
            CompanyDetailsSearchResponseDto companyDto = Optional.ofNullable(
                    companyClient.getCompanyByManagerId(userId).getBody()
            ).orElseThrow(() -> new DeliveryException(ErrorCode.COMPANY_NOT_FOUND));

            UUID companyId = companyDto.id();
            // 담당하는 업체의 배송이 아닌 경우
            if (!delivery.getCompanyId().equals(companyId)) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        return deliveryApplicationMapper.toDeliveryResponseServiceDto(delivery);
    }

    /**
     * 배송 검색 - 허브 관리자, 허브 배송 담당자, 업체 배송 담당자, 업체 담당자
     * @param userId 사용자 id
     * @param userRole 사용자 권한
     * @param requestServiceDto 배송 검색 request
     * @return 배송 검색 response
     */
    @Transactional(readOnly =true)
    @Cacheable(cacheNames = "deliveries")
    public Page<DeliveryResponseServiceDto> GetDeliveriesBySearch(Long userId,
                                                                  UserRoleType userRole,
                                                                  DeliverySearchRequestServiceDto requestServiceDto) {
        UUID hubId = null;
        UUID companyId = null;

        // 허브 관리자 : 본인이 담당하는 허브 id 조회
        if (userRole == UserRoleType.HUB_MANAGER) {
            HubSearchResponseDto hubDto = Optional.ofNullable(
                    hubClient.getHubByManagerId(userId).getBody()
            ).orElseThrow(() -> new DeliveryException(ErrorCode.HUB_NOT_FOUND));
            hubId = hubDto.id();
        }

        // 업체 담당자 : 본인이 담당하는 업체 id 조회
        if (userRole == UserRoleType.COMPANY_MANAGER) {
            CompanyDetailsSearchResponseDto companyDto = Optional.ofNullable(
                    companyClient.getCompanyByManagerId(userId).getBody()
            ).orElseThrow(() -> new DeliveryException(ErrorCode.COMPANY_NOT_FOUND));
            companyId = companyDto.id();
        }

        DeliverySearchCriteria criteria = null;

        if (userRole.equals(UserRoleType.HUB_DELIVERY_MANAGER) || userRole.equals(UserRoleType.COMPANY_DELIVERY_MANAGER)) {
            // 허브 배송 담당자, 업체 배송 담당자는 managerId 값에 본인 userId를 넣어 조회
            criteria = createDeliverySearchCriteria(
                    requestServiceDto.id(),
                    requestServiceDto.orderId(),
                    requestServiceDto.orderDetailId(),
                    requestServiceDto.hubId(),
                    requestServiceDto.companyId(),
                    requestServiceDto.status(),
                    userId
            );
        } else {
            // 허브 관리자, 업체 담당자는 본인이 담당하는 허브, 업체 id를 넣어 조회
            criteria = createDeliverySearchCriteria(
                    requestServiceDto.id(),
                    requestServiceDto.orderId(),
                    requestServiceDto.orderDetailId(),
                    hubId,
                    companyId,
                    requestServiceDto.status(),
                    requestServiceDto.managerId()
            );
        }

        Page<Delivery> deliveries = deliveryRepository.searchDelivery(
                criteria,
                requestServiceDto.customPageable());

        return deliveries.map(deliveryApplicationMapper::toDeliveryResponseServiceDto);
    }

    /**
     * 배송 경로 상세 조회 - 허브 관리자, 허브 배송 담당자, 업체 배송 담당자, 업체 담당자
     * @param userId 사용자 id
     * @param userRole 사용자 권한
     * @param requestServiceDto 배송 경로 상세 조회 request
     * @return 배송 경로 상세 조회 response
     */
    @Transactional(readOnly =true)
    @Cacheable(cacheNames = "route", key = "#requestServiceDto.id()")
    public DeliveryRouteResponseServiceDto GetDeliveryRouteDetail(Long userId,
                                                                  UserRoleType userRole,
                                                                  DeliveryRouteDetailRequestServiceDto requestServiceDto) {
        DeliveryRoute route = deliveryRepository.findRouteByIdAndIsDeletedFalse(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.ROUTE_NOT_FOUND));

        // 허브 관리자 : 본인이 담당하는 허브에서 출발하는 배송 경로인 지 유효성 검사
        if (userRole == UserRoleType.HUB_MANAGER) {
            HubSearchResponseDto hubDto = Optional.ofNullable(hubClient.getHubByManagerId(userId).getBody())
                    .orElseThrow(() -> new DeliveryException(ErrorCode.HUB_NOT_FOUND));

            UUID hubId = hubDto.id();
            // 허브 관리자의 담당하는 허브에서 출발하는 배송 경로가 아닌 경우
            if (route.getDepartureHubId() != hubId) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 허브 배송 담당자 : 본인이 담당하는 배송 경로인 지 유효성 검사
        if (userRole == UserRoleType.HUB_DELIVERY_MANAGER) {
            // 본인이 담당하는 배송 경로가 아닌 경우
            if (!route.getManager().getManagerId().equals(userId)) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 업체 배송 담당자 : 본인이 담당하는 배송의 마지막 배송 경로인 지 유효성 검사
        if (userRole == UserRoleType.COMPANY_DELIVERY_MANAGER) {
            // 본인이 담당하는 배송이 아닌 경우
            if (!route.getDelivery().getManager().getManagerId().equals(userId)) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
            // 업체 배송 담당자가 출발하는 허브와 배송 경로의 목적지 허브가 다른 경우
            if (!route.getArriveHubId().equals(route.getDelivery().getManager().getHubId())) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 업체 담당자 : 본인이 담당하는 업체의 배송에 속한 배송 경로인 지 유효성 검사
        if (userRole == UserRoleType.COMPANY_MANAGER) {
            CompanyDetailsSearchResponseDto companyDto = Optional.ofNullable(
                    companyClient.getCompanyByManagerId(userId).getBody()
            ).orElseThrow(() -> new DeliveryException(ErrorCode.COMPANY_NOT_FOUND));

            UUID companyId = companyDto.id();
            // 담당하는 업체의 배송에 속하는 배송 경로가 아닌 경우
            if (!route.getDelivery().getCompanyId().equals(companyId)) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        return deliveryApplicationMapper.toRouteResponseServiceDto(route);
    }

    /**
     * 배송 경로 검색 - 허브 관리자, 허브 배송 담당자, 업체 배송 담당자, 업체 담당자
     * @param userId 사용자 id
     * @param userRole 사용자 권한
     * @param requestServiceDto 배송 경로 검색 request
     * @return 배송 경로 검색 response
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "routes")
    public Page<DeliveryRouteResponseServiceDto> GetDeliveryRoutesBySearch(Long userId,
                                                                           UserRoleType userRole,
                                                                           DeliveryRouteSearchRequestServiceDto requestServiceDto) {
        UUID hubId = null;
        UUID companyId = null;

        // 허브 관리자 : 본인이 담당하는 허브 id 조회
        if (userRole == UserRoleType.HUB_MANAGER) {
            HubSearchResponseDto hubDto = Optional.ofNullable(
                    hubClient.getHubByManagerId(userId).getBody()
            ).orElseThrow(() -> new DeliveryException(ErrorCode.HUB_NOT_FOUND));
            hubId = hubDto.id();
        }

        // 업체 담당자 : 본인이 담당하는 업체 id 조회
        if (userRole == UserRoleType.COMPANY_MANAGER) {
            CompanyDetailsSearchResponseDto companyDto = Optional.ofNullable(
                    companyClient.getCompanyByManagerId(userId).getBody()
            ).orElseThrow(() -> new DeliveryException(ErrorCode.COMPANY_NOT_FOUND));
            companyId = companyDto.id();
        }

        DeliveryRouteSearchCriteria criteria = null;
        if (userRole.equals(UserRoleType.HUB_DELIVERY_MANAGER) || userRole.equals(UserRoleType.COMPANY_DELIVERY_MANAGER)) {
            // 허브 배송 담당자, 업체 배송 담당자는 managerId 값에 본인 userId를 넣어 조회
            criteria = createDeliveryRouteSearchCriteria(
                    requestServiceDto.routeId(),
                    requestServiceDto.orderId(),
                    requestServiceDto.orderDetailId(),
                    requestServiceDto.deliveryId(),
                    requestServiceDto.departureHubId(),
                    requestServiceDto.arriveHubId(),
                    requestServiceDto.companyId(),
                    userId,
                    requestServiceDto.status()
            );
        } else {
            // 허브 관리자, 업체 담당자는 본인이 담당하는 허브, 업체 id를 넣어 조회
            criteria = createDeliveryRouteSearchCriteria(
                    requestServiceDto.routeId(),
                    requestServiceDto.orderId(),
                    requestServiceDto.orderDetailId(),
                    requestServiceDto.deliveryId(),
                    hubId,
                    requestServiceDto.arriveHubId(),
                    companyId,
                    requestServiceDto.managerId(),
                    requestServiceDto.status()
            );
        }

        Page<DeliveryRoute> routes = deliveryRepository.searchRoute(
                criteria,
                requestServiceDto.customPageable());

        return routes.map(deliveryApplicationMapper::toRouteResponseServiceDto);

    }

    /**
     * 배송 경로 상태 수정 - 허브 관리자, 허브 배송 담당자, 업체 배송 담당자
     * @param userId 사용자 id
     * @param userRole 사용자 권한
     * @param requestServiceDto 배송 경로 상태 수정 request
     * @return 배송 경로 상태 수정 response
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "route", key = "#requestServiceDto.id()"),
            @CacheEvict(cacheNames = "routes", allEntries = true)
    })
    public DeliveryRouteUpdateStatusResponseServiceDto updateRouteStatusDelivery(Long userId,
                                                                                 UserRoleType userRole,
                                                                                 DeliveryRouteUpdateStatusRequestServiceDto requestServiceDto) {
        DeliveryRoute route = deliveryRepository.findRouteByIdAndIsDeletedFalse(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.ROUTE_NOT_FOUND));

        // 허브 관리자 : 본인이 담당하는 허브에서 출발하는 배송 경로인 지 유효성 검사
        if (userRole == UserRoleType.HUB_MANAGER) {
            HubSearchResponseDto hubDto = Optional.ofNullable(hubClient.getHubByManagerId(userId).getBody())
                    .orElseThrow(() -> new DeliveryException(ErrorCode.HUB_NOT_FOUND));

            UUID hubId = hubDto.id();
            // 허브 관리자의 담당하는 허브에서 출발하는 배송 경로가 아닌 경우
            if (route.getDepartureHubId() != hubId) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 허브 배송 담당자 : 본인이 담당하는 배송 경로인 지 유효성 검사
        if (userRole == UserRoleType.HUB_DELIVERY_MANAGER) {
            // 본인이 담당하는 배송 경로가 아닌 경우
            if (!route.getManager().getManagerId().equals(userId)) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 업체 배송 담당자 : 본인이 담당하는 배송의 마지막 배송 경로인 지 유효성 검사
        if (userRole == UserRoleType.COMPANY_DELIVERY_MANAGER) {
            // 본인이 담당하는 배송이 아닌 경우
            if (!route.getDelivery().getManager().getManagerId().equals(userId)) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
            // 업체 배송 담당자가 출발하는 허브와 배송 경로의 목적지 허브가 다른 경우
            if (!route.getArriveHubId().equals(route.getDelivery().getManager().getHubId())) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        // 배송 경로 상태와 변경하려는 상태가 같은 경우
        if (route.getStatus().equals(requestServiceDto.status())) {
            throw new DeliveryException(ErrorCode.BAD_REQUEST);
        }

        route.changeStatus(requestServiceDto.status());

        if (route.getStatus() == DeliveryRouteStatus.HUB_MOVING) {         // 허브 이동중 상태로 변경 시도하는 경우
            Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(route.getDelivery().getId())
                    .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

            if (delivery.getArriveHubId() == route.getArriveHubId()) {      // 경로 상 출발지 허브가 배송 출발지 허브와 같으면 배송 상태 변경
                delivery.updateStatus(DeliveryStatus.HUB_MOVING);
            }
        }

        if (route.getStatus() == DeliveryRouteStatus.HUB_ARRIVED) {         // 목적지 허브 도착 상태로 변경 시도하는 경우
            Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(route.getDelivery().getId())
                    .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

            if (delivery.getArriveHubId() == route.getArriveHubId()) {      // 경로 상 목적지 허브가 배송 목적지 허브와 같으면 배송 상태 변경
                delivery.updateStatus(DeliveryStatus.HUB_ARRIVED);
            }
        }

        return deliveryApplicationMapper.toUpdateRouteStatusResponseServiceDto(route.getId());

    }

    @Transactional(readOnly = true)
    public DeliveryManagerResponseServiceDto GetDeliveryManagerDetail(Long userId,
                                                                      UserRoleType userRole,
                                                                      DeliveryManagerDetailRequestServiceDto requestServiceDto) {
        DeliveryManager manager = deliveryManagerRepository.findByIdAndIsDeletedFalse(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.MANAGER_NOT_FOUND));

        // 허브 관리자 : 본인이 담당하는 허브 id 조회
        if (userRole == UserRoleType.HUB_MANAGER) {
            HubSearchResponseDto hubDto = Optional.ofNullable(
                    hubClient.getHubByManagerId(userId).getBody()
            ).orElseThrow(() -> new DeliveryException(ErrorCode.HUB_NOT_FOUND));

            if (hubDto.id() != manager.getId()) {
                throw new DeliveryException(ErrorCode.UNAUTHORIZED);
            }
        }

        return deliveryApplicationMapper.toManagerResponseServiceDto(manager);

    }


    @Transactional(readOnly = true)
    public Page<DeliveryManagerResponseServiceDto> GetDeliveryManagerBySearch(Long userId,
                                                                              UserRoleType userRole,
                                                                              DeliveryManagerSearchRequestServiceDto requestServiceDto) {
        UUID hubId = null;

        // 허브 관리자 : 본인이 담당하는 허브 id 조회
        if (userRole == UserRoleType.HUB_MANAGER) {
            HubSearchResponseDto hubDto = Optional.ofNullable(
                    hubClient.getHubByManagerId(userId).getBody()
            ).orElseThrow(() -> new DeliveryException(ErrorCode.HUB_NOT_FOUND));
            hubId = hubDto.id();
        }

        DeliveryManagerSearchCriteria criteria = null;
        if (userRole.equals(UserRoleType.HUB_DELIVERY_MANAGER) || userRole.equals(UserRoleType.COMPANY_DELIVERY_MANAGER)) {
            // 허브 배송 담당자, 업체 배송 담당자는 managerId 값에 본인 userId를 넣어 조회
            criteria = createDeliveryManagerSearchCriteria(
                    requestServiceDto.id(),
                    requestServiceDto.slackId(),
                    requestServiceDto.hubId(),
                    userId,
                    requestServiceDto.type(),
                    requestServiceDto.sequence()
            );
        } else {
            // 허브 관리자, 업체 담당자는 본인이 담당하는 허브 id를 넣어 조회
            criteria = createDeliveryManagerSearchCriteria(
                    requestServiceDto.id(),
                    requestServiceDto.slackId(),
                    hubId,
                    requestServiceDto.managerId(),
                    requestServiceDto.type(),
                    requestServiceDto.sequence()
            );
        }

        Page<DeliveryManager> managers = deliveryRepository.searchManager(
                criteria,
                requestServiceDto.customPageable());

        return managers.map(deliveryApplicationMapper::toManagerResponseServiceDto);

    }



    // 배송 조회 검색 조건 생성 (일반 사용자)
    private DeliverySearchCriteria createDeliverySearchCriteria(UUID id,
                                                                UUID orderId,
                                                                UUID orderDetailId,
                                                                UUID hubId,
                                                                UUID companyId,
                                                                DeliveryStatus status,
                                                                Long managerId) {
        return DeliverySearchCriteria.builder()
                .id(id)
                .orderId(orderId)
                .orderDetailId(orderDetailId)
                .hubId(hubId)
                .companyId(companyId)
                .status(status)
                .managerId(managerId)       // 배송 담당자 id (Long)
                .isDeleted(Boolean.FALSE)   // 삭제되지 않은 데이터만 조회
                .build();
    }

    // 배송 경로 조회 검색 조건 생성 (일반 사용자)
    private DeliveryRouteSearchCriteria createDeliveryRouteSearchCriteria(UUID routeId,
                                                                          UUID orderId,
                                                                          UUID orderDetailId,
                                                                          UUID deliveryId,
                                                                          UUID departureHubId,
                                                                          UUID arriveHubId,
                                                                          UUID companyId,
                                                                          Long managerId,
                                                                          DeliveryRouteStatus status) {
        return DeliveryRouteSearchCriteria.builder()
                .routeId(routeId)
                .orderId(orderId)
                .orderDetailId(orderDetailId)
                .deliveryId(deliveryId)
                .departureHubId(departureHubId)
                .arriveHubId(arriveHubId)
                .companyId(companyId)
                .managerId(managerId)       // 배송 담당자 id (Long)
                .status(status)
                .isDeleted(Boolean.FALSE)   // 삭제되지 않은 데이터만 조회
                .build();
    }

    // 배송 담당자 검색 조건 생성 (일반 사용자)
    private DeliveryManagerSearchCriteria createDeliveryManagerSearchCriteria(UUID id,
                                                                              String slackId,
                                                                              UUID hubId,
                                                                              Long managerId,
                                                                              DeliveryManagerType type,
                                                                              Integer sequence) {
        return DeliveryManagerSearchCriteria.builder()
                .id(id)
                .slackId(slackId)
                .hubId(hubId)
                .managerId(managerId)
                .type(type)
                .sequence(sequence)
                .isDeleted(Boolean.FALSE)
                .build();
    }
}
