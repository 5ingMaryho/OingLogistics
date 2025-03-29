package com.oingmaryho.business.delivery_service.application.service;

import com.oingmaryho.business.common.domain.type.UserRoleType;
import com.oingmaryho.business.delivery_service.application.DeliveryLockHelper;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAdminService {

    private final HubClient hubClient;
    private final UserClient userClient;

    private final RedisTemplate<String, Object> redisTemplate;

    private final DeliveryRepository deliveryRepository;
    private final DeliveryManagerRepository deliveryManagerRepository;
    private final DeliveryApplicationMapper deliveryApplicationMapper;

    // --- helper --- //
    private final DeliveryLockHelper deliveryLockHelper;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeliveryManagerAssignmentRequestServiceDto createDelivery(
            DeliveryCreationRequestServiceDto requestServiceDto) {

        // 1. 최적 경로 조회
        List<HubPathResponseDto> hubRoutes = Optional.ofNullable(
                hubClient.getPath(requestServiceDto.hubId(), requestServiceDto.address()).getBody()
        ).orElseThrow(() -> new DeliveryException(ErrorCode.HUB_PATH_NOT_FOUND));


        // --- logging --- //
        for (int i = 0; i < hubRoutes.size(); i++) {
            log.info("route{} : departureId({}) arriveId({}) time({}) dist({})",
                    i,
                    hubRoutes.get(i).departureHubId(),
                    hubRoutes.get(i).arriveHubId(),
                    hubRoutes.get(i).hubToHubTime(),
                    hubRoutes.get(i).distance());
        }

        // 2. 배송 생성
        Delivery delivery = Delivery.builder()
                .orderId(requestServiceDto.orderId())
                .orderDetailId(requestServiceDto.orderDetailId())
                .companyId(requestServiceDto.companyId())
                .departureHubId(hubRoutes.get(0).departureHubId())
                .departureHubName(hubRoutes.get(0).departureHubName())
                .arriveHubId(hubRoutes.get(hubRoutes.size()-1).arriveHubId())
                .arriveHubName(hubRoutes.get(hubRoutes.size()-1).arriveHubName())
                .address(requestServiceDto.address())
                .receiver(requestServiceDto.receiver())
                .receiverSlackId(requestServiceDto.receiverSlackId())
                .build();

        // 3. 배송 경로 생성
        int routeSequence = 0;
        for (HubPathResponseDto route : hubRoutes) {
            DeliveryRoute deliveryRoute = DeliveryRoute.builder()
                    .delivery(delivery)
                    .departureHubId(route.departureHubId())
                    .departureHubName(route.departureHubName())
                    .arriveHubId(route.arriveHubId())
                    .departureHubName(route.departureHubName())
                    .arriveHubName(route.arriveHubName())
                    .status(DeliveryRouteStatus.HUB_WAITING)
                    .sequence(routeSequence++)
                    .estimatedDistance(route.distance())
                    .estimatedTime(route.hubToHubTime())
                    .build();

            deliveryRoute.addRoute(delivery);
        }

        Delivery savedDelivery = deliveryRepository.save(delivery);

        return deliveryApplicationMapper.toManagerAssignmentRequestServiceDto(
                savedDelivery.getId()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeliveryManagerAssignmentRequestServiceDto assignHubDeliveryManager(
            DeliveryManagerAssignmentRequestServiceDto requestServiceDto) {

        Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(requestServiceDto.deliveryId())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

        List<DeliveryRoute> hubRoutes = delivery.getRoutes();

        String hubDeliveryManagerSequenceKey = deliveryLockHelper.getHubDeliveryManagerSequenceKey();
        Object backupValue = redisTemplate.opsForValue().get(hubDeliveryManagerSequenceKey);
        int backupHubDeliveryManagerSequence = Optional.ofNullable(backupValue)
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(0);

        String hubLockValue = UUID.randomUUID().toString();
        boolean hubLocked = deliveryLockHelper.tryHubManagerLock(hubLockValue, 5);

        if (!hubLocked) {
            throw new DeliveryException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        redisTemplate.opsForValue().setIfAbsent(hubDeliveryManagerSequenceKey, 0);

        try {
            for (int sequence = 0; sequence < hubRoutes.size(); sequence++) {
                DeliveryRoute route = hubRoutes.get(sequence);

                int hubDeliveryManagerSequence = Optional.ofNullable(redisTemplate.opsForValue().get(hubDeliveryManagerSequenceKey))
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElse(0);

                // 허브 배송 담당자 - 전체 물류 시스템에 10명 - 순차 배정
                DeliveryManager hubDeliveryManager = deliveryManagerRepository.findByTypeAndSequence(
                                DeliveryManagerType.HUB_DELIVERY_MANAGER,
                                hubDeliveryManagerSequence % 10)
                        .orElseThrow(() -> new DeliveryException(ErrorCode.MANAGER_NOT_FOUND));

                route.assignManager(sequence, hubDeliveryManager);

                redisTemplate.opsForValue().increment(hubDeliveryManagerSequenceKey);

            }
        } catch (DeliveryException e) {
            redisTemplate.opsForValue().set(hubDeliveryManagerSequenceKey, backupHubDeliveryManagerSequence);
            throw new DeliveryException(ErrorCode.DELIVERY_MANAGER_NOT_ASSIGNED);
        } catch (Exception e) {
            redisTemplate.opsForValue().set(hubDeliveryManagerSequenceKey, backupHubDeliveryManagerSequence);
            throw new DeliveryException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            deliveryLockHelper.releaseHubManagerLock(hubLockValue);
        }

        return deliveryApplicationMapper.toManagerAssignmentRequestServiceDto(
                delivery.getId()
        );

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderMessageCreationRequestServiceDto assignCompanyDeliveryManager(
            DeliveryManagerAssignmentRequestServiceDto requestServiceDto) {

        Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(requestServiceDto.deliveryId())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

        UUID arriveHubId = delivery.getArriveHubId();

        String companyDeliveryManagerSequenceKey = deliveryLockHelper.getCompanyDeliveryManagerSequenceKey(arriveHubId);
        Object backupValue = redisTemplate.opsForValue().get(companyDeliveryManagerSequenceKey);
        int backupCompanyDeliveryManagerSequence = Optional.ofNullable(backupValue)
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(0);

        String companyLockValue = UUID.randomUUID().toString();
        boolean companyLocked = deliveryLockHelper.tryCompanyManagerLock(arriveHubId, companyLockValue, 5);

        if (!companyLocked) {
            throw new DeliveryException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        redisTemplate.opsForValue().setIfAbsent(companyDeliveryManagerSequenceKey, 0);

        try {
            int companyDeliveryManagerSequence = Optional.ofNullable(redisTemplate.opsForValue().get(companyDeliveryManagerSequenceKey))
                    .map(Object::toString)
                    .map(Integer::parseInt)
                    .orElse(0);

            // 배송 경로 기준 마지막 경로의 도착지 허브에 있는 업체 배송 담당자 10명 - 순차 배정
            DeliveryManager companyDeliveryManager = deliveryManagerRepository.findByHubIdAndTypeAndSequence(
                            arriveHubId, DeliveryManagerType.COMPANY_DELIVERY_MANAGER, companyDeliveryManagerSequence % 10)
                    .orElseThrow(() -> new DeliveryException(ErrorCode.MANAGER_NOT_FOUND));

            delivery.update(null, null, null, companyDeliveryManager);

            redisTemplate.opsForValue().increment(companyDeliveryManagerSequenceKey);

        } catch (DeliveryException e) {
            redisTemplate.opsForValue().set(companyDeliveryManagerSequenceKey, backupCompanyDeliveryManagerSequence);
            throw new DeliveryException(ErrorCode.DELIVERY_MANAGER_NOT_ASSIGNED);
        } catch (Exception e) {
            redisTemplate.opsForValue().set(companyDeliveryManagerSequenceKey, backupCompanyDeliveryManagerSequence);
            throw new DeliveryException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            deliveryLockHelper.releaseCompanyManagerLock(arriveHubId, companyLockValue);
        }

        return deliveryApplicationMapper.toOrderMessageCreationRequestServiceDto(
                delivery.getId()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeliveryCreationResponseServiceDto createMessageToOrder(
            OrderMessageCreationRequestServiceDto requestServiceDto) {

        Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(requestServiceDto.deliveryId())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

        List<DeliveryRoute> hubRoutes = delivery.getRoutes();

        StringBuilder hubNames = new StringBuilder(delivery.getDepartureHubName());

        for (DeliveryRoute route: hubRoutes) {
            hubNames.append(",").append(route.getArriveHubId());
        }

        // 업체 배송 담당자 이름 조회
        String companyDeliveryManagerName = Optional.ofNullable(userClient.getUserName(delivery.getManager().getManagerId()).getBody())
                .orElseThrow(() -> new DeliveryException(ErrorCode.USER_NAME_NOT_FOUND));

        return deliveryApplicationMapper.toCreationResponseServiceDto(
                delivery.getOrderId(),
                delivery.getOrderDetailId(),
                delivery.getId(),
                delivery.getDepartureHubName(),
                hubNames.toString(),
                delivery.getArriveHubName(),
                companyDeliveryManagerName,
                delivery.getManager().getSlackId()
        );

    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "delivery", key = "#requestServiceDto.id()"),
            @CacheEvict(cacheNames = "deliveries", allEntries = true)
    })
    public DeliveryUpdateResponseServiceDto updateDelivery(
            Long userId,
            UserRoleType userRole,
            DeliveryUpdateRequestServiceDto requestServiceDto) {

        Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

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

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "delivery", key = "#requestServiceDto.id()"),
            @CacheEvict(cacheNames = "deliveries", allEntries = true)
    })
    public DeliveryUpdateStatusResponseServiceDto updateStatusDelivery(
            Long userId,
            UserRoleType userRole,
            DeliveryUpdateStatusRequestServiceDto requestServiceDto) {

        Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

        // 배송 상태와 변경하려는 상태가 같은 경우
        if (delivery.getStatus().equals(requestServiceDto.status())) {
            throw new DeliveryException(ErrorCode.BAD_REQUEST);
        }

        delivery.updateStatus(requestServiceDto.status());
        return deliveryApplicationMapper.toUpdateStatusResponseServiceDto(delivery.getId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "delivery", key = "#requestServiceDto.id()"),
            @CacheEvict(cacheNames = "deliveries", allEntries = true)
    })
    public void deleteDelivery(
            Long userId,
            UserRoleType userRole,
            DeliveryDeletionRequestServiceDto requestServiceDto) {

        Delivery delivery = deliveryRepository.findByIdAndIsDeletedFalse(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));
        delivery.softDelete(userId);

    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "delivery", key = "#requestServiceDto.id()")
    public DeliveryResponseServiceDto GetDeliveryDetail(
            Long userId,
            UserRoleType userRole,
            DeliveryDetailRequestServiceDto requestServiceDto) {

        Delivery delivery = deliveryRepository.findById(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.DELIVERY_NOT_FOUND));

        return deliveryApplicationMapper.toDeliveryResponseServiceDto(delivery);
    }

    @Transactional(readOnly =true)
    @Cacheable(cacheNames = "deliveries")
    public Page<DeliveryResponseServiceDto> GetDeliveriesBySearch(
            Long userId,
            UserRoleType userRole,
            DeliverySearchRequestServiceDto requestServiceDto) {

        DeliverySearchCriteria criteria = createDeliverySearchCriteria(
                requestServiceDto.id(),
                requestServiceDto.orderId(),
                requestServiceDto.orderDetailId(),
                requestServiceDto.hubId(),
                requestServiceDto.companyId(),
                requestServiceDto.status(),
                requestServiceDto.managerId(),
                requestServiceDto.isDeleted()
        );

        Page<Delivery> deliveries = deliveryRepository.searchDelivery(
                criteria,
                requestServiceDto.customPageable());

        return deliveries.map(deliveryApplicationMapper::toDeliveryResponseServiceDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "route", key = "#requestServiceDto.id()")
    public DeliveryRouteResponseServiceDto GetDeliveryRouteDetail(
            Long userId,
            UserRoleType userRole,
            DeliveryRouteDetailRequestServiceDto requestServiceDto) {

        DeliveryRoute route = deliveryRepository.findRouteById(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.ROUTE_NOT_FOUND));

        return deliveryApplicationMapper.toRouteResponseServiceDto(route);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "routes")
    public Page<DeliveryRouteResponseServiceDto> GetDeliveryRoutesBySearch(
            Long userId,
            UserRoleType userRole,
            DeliveryRouteSearchRequestServiceDto requestServiceDto) {

        DeliveryRouteSearchCriteria criteria = createDeliveryRouteSearchCriteria(
                requestServiceDto.routeId(),
                requestServiceDto.orderId(),
                requestServiceDto.orderDetailId(),
                requestServiceDto.deliveryId(),
                requestServiceDto.departureHubId(),
                requestServiceDto.arriveHubId(),
                requestServiceDto.companyId(),
                requestServiceDto.managerId(),
                requestServiceDto.status(),
                requestServiceDto.isDeleted()
        );


        Page<DeliveryRoute> routes = deliveryRepository.searchRoute(
                criteria,
                requestServiceDto.customPageable());

        return routes.map(deliveryApplicationMapper::toRouteResponseServiceDto);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "route", key = "#requestServiceDto.id()"),
            @CacheEvict(cacheNames = "routes", allEntries = true)
    })
    public DeliveryRouteUpdateStatusResponseServiceDto updateRouteStatusDelivery(
            Long userId,
            UserRoleType userRole,
            DeliveryRouteUpdateStatusRequestServiceDto requestServiceDto) {

        DeliveryRoute route = deliveryRepository.findRouteById(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.ROUTE_NOT_FOUND));

        // 배송 경로 상태와 변경하려는 상태가 같은 경우
        if (route.getStatus().equals(requestServiceDto.status())) {
            throw new DeliveryException(ErrorCode.BAD_REQUEST);
        }

        route.changeStatus(requestServiceDto.status());

        // 허브 이동중 상태로 변경 시도하는 경우
        if (route.getStatus() == DeliveryRouteStatus.HUB_MOVING) {
            Delivery delivery = route.getDelivery();

            if (delivery.getIsDeleted()) {
                throw new DeliveryException(ErrorCode.BAD_REQUEST);
            }

            // 경로 상 출발지 허브가 배송 출발지 허브와 같으면 배송 상태 변경
            if (delivery.getArriveHubId() == route.getArriveHubId()) {
                delivery.updateStatus(DeliveryStatus.HUB_MOVING);
            }
        }
        // 목적지 허브 도착 상태로 변경 시도하는 경우
        if (route.getStatus() == DeliveryRouteStatus.HUB_ARRIVED) {
            Delivery delivery = route.getDelivery();

            if (delivery.getIsDeleted()) {
                throw new DeliveryException(ErrorCode.BAD_REQUEST);
            }

            // 경로 상 목적지 허브가 배송 목적지 허브와 같으면 배송 상태 변경
            if (delivery.getArriveHubId() == route.getArriveHubId()) {
                delivery.updateStatus(DeliveryStatus.HUB_ARRIVED);
            }
        }

        return deliveryApplicationMapper.toUpdateRouteStatusResponseServiceDto(route.getId());

    }

    @Transactional(readOnly = true)
    public DeliveryManagerResponseServiceDto GetDeliveryManagerDetail(
            Long userId,
            String userRole,
            DeliveryManagerDetailRequestServiceDto requestServiceDto) {

        DeliveryManager manager = deliveryManagerRepository.findById(requestServiceDto.id())
                .orElseThrow(() -> new DeliveryException(ErrorCode.MANAGER_NOT_FOUND));

        return deliveryApplicationMapper.toManagerResponseServiceDto(manager);

    }


    @Transactional(readOnly = true)
    public Page<DeliveryManagerResponseServiceDto> GetDeliveryManagerBySearch(
            Long userId,
            String userRole,
            DeliveryManagerSearchRequestServiceDto requestServiceDto) {

        DeliveryManagerSearchCriteria criteria = createDeliveryManagerSearchCriteria(
                requestServiceDto.id(),
                requestServiceDto.slackId(),
                requestServiceDto.hubId(),
                requestServiceDto.managerId(),
                requestServiceDto.type(),
                requestServiceDto.sequence(),
                requestServiceDto.isDeleted()
        );

        Page<DeliveryManager> managers = deliveryRepository.searchManager(
                criteria,
                requestServiceDto.customPageable());

        return managers.map(deliveryApplicationMapper::toManagerResponseServiceDto);

    }

    // 배송 조회 검색 조건 생성 (admin)
    private DeliverySearchCriteria createDeliverySearchCriteria(
            UUID id,
            UUID orderId,
            UUID orderDetailId,
            UUID hubId,
            UUID companyId,
            DeliveryStatus status,
            Long managerId,
            Boolean isDeleted) {

        return DeliverySearchCriteria.builder()
                .id(id)
                .orderId(orderId)
                .orderDetailId(orderDetailId)
                .hubId(hubId)
                .companyId(companyId)
                .status(status)
                .managerId(managerId)                               // 배송 담당자 id (Long)
                .isDeleted(isDeleted == null ? null:                // 전체 조회
                        isDeleted ?  Boolean.TRUE : Boolean.FALSE)  // 필터 조건이 들어올 경우 해당하는 결과만 조회
                .build();
    }

    // 배송 경로 조회 검색 조건 생성 (admin)
    private DeliveryRouteSearchCriteria createDeliveryRouteSearchCriteria(
            UUID routeId,
            UUID orderId,
            UUID orderDetailId,
            UUID deliveryId,
            UUID departureHubId,
            UUID arriveHubId,
            UUID companyId,
            Long managerId,
            DeliveryRouteStatus status,
            Boolean isDeleted) {


        return DeliveryRouteSearchCriteria.builder()
                .routeId(routeId)
                .orderId(orderId)
                .orderDetailId(orderDetailId)
                .deliveryId(deliveryId)
                .departureHubId(departureHubId)
                .arriveHubId(arriveHubId)
                .companyId(companyId)
                .managerId(managerId)                               // 배송 담당자 id (Long)
                .status(status)
                .isDeleted(isDeleted == null ? null:                // 전체 조회
                        isDeleted ?  Boolean.TRUE : Boolean.FALSE)  // 필터 조건이 들어올 경우 해당하는 결과만 조회
                .build();
    }

    // 배송 담당자 검색 조건 생성 (admin)
    private DeliveryManagerSearchCriteria createDeliveryManagerSearchCriteria(
            UUID id,
            String slackId,
            UUID hubId,
            Long managerId,
            DeliveryManagerType type,
            Integer sequence,
            Boolean isDeleted) {


        return DeliveryManagerSearchCriteria.builder()
                .id(id)
                .slackId(slackId)
                .hubId(hubId)
                .managerId(managerId)
                .type(type)
                .sequence(sequence)
                .isDeleted(isDeleted)
                .build();
    }

}
