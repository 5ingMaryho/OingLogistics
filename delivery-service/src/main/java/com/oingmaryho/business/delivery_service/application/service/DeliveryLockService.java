package com.oingmaryho.business.delivery_service.application.service;

import com.oingmaryho.business.delivery_service.application.dto.context.CompanyAssignmentContext;
import com.oingmaryho.business.delivery_service.application.dto.context.HubAssignmentContext;
import com.oingmaryho.business.delivery_service.application.dto.mapper.DeliveryApplicationMapper;
import com.oingmaryho.business.delivery_service.application.dto.request.DeliveryManagerAssignmentRequestServiceDto;
import com.oingmaryho.business.delivery_service.application.dto.request.OrderMessageCreationRequestServiceDto;
import com.oingmaryho.business.delivery_service.domain.entity.Delivery;
import com.oingmaryho.business.delivery_service.domain.entity.DeliveryManager;
import com.oingmaryho.business.delivery_service.domain.entity.DeliveryRoute;
import com.oingmaryho.business.delivery_service.domain.repository.DeliveryManagerRepository;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryManagerType;
import com.oingmaryho.business.delivery_service.exception.DeliveryException;
import com.oingmaryho.business.delivery_service.exception.ErrorCode;
import com.oingmaryho.business.delivery_service.infrastructure.DistributedLock;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryLockService {

    private final DeliveryManagerRepository deliveryManagerRepository;
    private final DeliveryApplicationMapper deliveryApplicationMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    @DistributedLock(key = "#lockKey")
    public DeliveryManagerAssignmentRequestServiceDto assignHubDeliveryManagerWithLock(
            String lockKey,
            String hubDeliveryManagerSequenceKey,
            Delivery delivery) {

        initializeSequenceIfAbsent(hubDeliveryManagerSequenceKey);

        int backupHubDeliveryManagerSequence = getSequenceValueOrDefault(hubDeliveryManagerSequenceKey);
        
        List<DeliveryRoute> hubRoutes = delivery.getRoutes();
        
        for (int sequence = 0; sequence < hubRoutes.size(); sequence++) {
            assignHubDeliveryManagerForRoute(
                    new HubAssignmentContext(
                            backupHubDeliveryManagerSequence,
                            hubDeliveryManagerSequenceKey,
                            hubRoutes,
                            sequence
                    )
            );
            backupHubDeliveryManagerSequence++;
        }
        
        return deliveryApplicationMapper.toManagerAssignmentRequestServiceDto(
                delivery.getId()
        );

    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000),
            retryFor  = {RuntimeException.class}
    )
    private void assignHubDeliveryManagerForRoute(
            HubAssignmentContext context) {

        DeliveryRoute route = context.getHubRoutes().get(context.getSequence());

        // 현재 순번 조회
        int hubDeliveryManagerSequence = getSequenceValueOrDefault(context.getSequenceKey());

        // 허브 배송 담당자 - 전체 물류 시스템에 10명 - 순차 배정
        DeliveryManager hubDeliveryManager = deliveryManagerRepository.findByTypeAndSequence(
                        DeliveryManagerType.HUB_DELIVERY_MANAGER,
                        hubDeliveryManagerSequence % 10)
                .orElseThrow(() -> new DeliveryException(ErrorCode.MANAGER_NOT_FOUND));

        route.assignManager(context.getSequence(), hubDeliveryManager);
        redisTemplate.opsForValue().increment(context.getSequenceKey());
    }

    @Recover
    public void recoverForAssignHubDeliveryManagerForRoute(
            RetryableException e,
            HubAssignmentContext context) {
        log.warn("Retry failed. Recovering hub delivery manager assignment. Redis key: {}, backupSeq: {}",
                context.getSequenceKey(), context.getBackupSequence());
        redisTemplate.opsForValue().set(context.getSequenceKey(), context.getBackupSequence());
        throw new DeliveryException(ErrorCode.HUB_DELIVERY_MANAGER_NOT_ASSIGNED);
    }

    @DistributedLock(key = "#lockKey")
    public OrderMessageCreationRequestServiceDto assignCompanyDeliveryManagerWithLock(
            String lockKey,
            String companyDeliveryManagerSequenceKey,
            Delivery delivery,
            UUID arriveHubId) {

        initializeSequenceIfAbsent(companyDeliveryManagerSequenceKey);

        int backupCompanyDeliveryManagerSequence = getSequenceValueOrDefault(companyDeliveryManagerSequenceKey);

        assignCompanyDeliveryManagerForDelivery(
                new CompanyAssignmentContext(
                        backupCompanyDeliveryManagerSequence,
                        companyDeliveryManagerSequenceKey,
                        delivery,
                        arriveHubId
                )
        );

        return deliveryApplicationMapper.toOrderMessageCreationRequestServiceDto(
                delivery.getId()
        );

    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000),
            retryFor  = {RuntimeException.class}
    )
    private void assignCompanyDeliveryManagerForDelivery(
            CompanyAssignmentContext context) {

        // 현재 순번 조회
        int companyDeliveryManagerSequence = getSequenceValueOrDefault(context.getSequenceKey());

        // 배송 경로 기준 마지막 경로의 도착지 허브에 있는 업체 배송 담당자 10명 - 순차 배정
        DeliveryManager companyDeliveryManager = deliveryManagerRepository.findByHubIdAndTypeAndSequence(
                        context.getArriveHubId(),
                        DeliveryManagerType.COMPANY_DELIVERY_MANAGER,
                        companyDeliveryManagerSequence % 10)
                .orElseThrow(() -> new DeliveryException(ErrorCode.MANAGER_NOT_FOUND));

        context.getDelivery().update(null, null, null, companyDeliveryManager);
        redisTemplate.opsForValue().increment(context.getSequenceKey());
    }

    @Recover
    public void recoverForAssignCompanyDeliveryManagerForDelivery(
            RetryableException e,
            CompanyAssignmentContext context) {
        log.warn("Retry failed. Recovering company delivery manager assignment. Redis key: {}, backupSeq: {}",
                context.getSequenceKey(), context.getBackupSequence());
        redisTemplate.opsForValue().set(context.getSequenceKey(), context.getBackupSequence());
        throw new DeliveryException(ErrorCode.COMPANY_DELIVERY_MANAGER_NOT_ASSIGNED);
    }

    private void initializeSequenceIfAbsent(String key) {
        redisTemplate.opsForValue().setIfAbsent(key, 0);
    }

    private int getSequenceValueOrDefault(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(0);
    }

}
