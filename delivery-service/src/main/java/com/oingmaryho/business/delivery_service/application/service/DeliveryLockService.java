package com.oingmaryho.business.delivery_service.application.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
            String lockKey, String hubDeliveryManagerSequenceKey, Delivery delivery) {

        Object backupValue = redisTemplate.opsForValue().get(hubDeliveryManagerSequenceKey);
        int backupHubDeliveryManagerSequence = Optional.ofNullable(backupValue)
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(0);

        redisTemplate.opsForValue().setIfAbsent(hubDeliveryManagerSequenceKey, 0);

        List<DeliveryRoute> hubRoutes = delivery.getRoutes();

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
            return deliveryApplicationMapper.toManagerAssignmentRequestServiceDto(
                    delivery.getId()
            );
        } catch (DeliveryException e) {
            redisTemplate.opsForValue().set(hubDeliveryManagerSequenceKey, backupHubDeliveryManagerSequence);
            throw new DeliveryException(ErrorCode.DELIVERY_MANAGER_NOT_ASSIGNED);
        } catch (Exception e) {
            redisTemplate.opsForValue().set(hubDeliveryManagerSequenceKey, backupHubDeliveryManagerSequence);
            throw e;
        }

    }

    @DistributedLock(key = "#lockKey")
    public OrderMessageCreationRequestServiceDto assignCompanyDeliveryManagerWithLock(
            String lockKey, String companyDeliveryManagerSequenceKey, Delivery delivery, UUID arriveHubId) {

        Object backupValue = redisTemplate.opsForValue().get(companyDeliveryManagerSequenceKey);
        int backupCompanyDeliveryManagerSequence = Optional.ofNullable(backupValue)
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(0);

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

            return deliveryApplicationMapper.toOrderMessageCreationRequestServiceDto(
                    delivery.getId()
            );
        } catch (DeliveryException e) {
            redisTemplate.opsForValue().set(companyDeliveryManagerSequenceKey, backupCompanyDeliveryManagerSequence);
            throw new DeliveryException(ErrorCode.DELIVERY_MANAGER_NOT_ASSIGNED);
        } catch (Exception e) {
            redisTemplate.opsForValue().set(companyDeliveryManagerSequenceKey, backupCompanyDeliveryManagerSequence);
            throw e;
        }

    }

}
