package com.oingmaryho.business.delivery_service.infrastructure.repository;

import com.oingmaryho.business.delivery_service.domain.criteria.DeliveryManagerSearchCriteria;
import com.oingmaryho.business.delivery_service.domain.criteria.DeliveryRouteSearchCriteria;
import com.oingmaryho.business.delivery_service.domain.criteria.DeliverySearchCriteria;
import com.oingmaryho.business.delivery_service.domain.entity.*;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryManagerType;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryRouteStatus;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryStatus;
import com.oingmaryho.business.delivery_service.utils.QueryDslUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryCustomRepositoryImpl implements DeliveryCustomRepository {
    private final JPAQueryFactory queryFactory;

    QDelivery qDelivery = QDelivery.delivery;
    QDeliveryRoute qDeliveryRoute = QDeliveryRoute.deliveryRoute;
    QDeliveryManager qDeliveryManager = QDeliveryManager.deliveryManager;
    BooleanBuilder builder = new BooleanBuilder();

    /**
     * 배송 검색
     * @param criteria 검색 조건
     * @param pageable customPageable
     * @return 배송
     */
    @Override
    public Page<Delivery> searchDelivery(DeliverySearchCriteria criteria,
                                         Pageable pageable) {

        UUID id = criteria.getId();
        UUID orderId = criteria.getOrderId();
        UUID orderDetailId = criteria.getOrderDetailId();
        UUID hubId = criteria.getHubId();
        UUID companyId = criteria.getCompanyId();
        DeliveryStatus status = criteria.getStatus();
        Long managerId = criteria.getManagerId();
        Boolean isDeleted = criteria.getIsDeleted();

        builder.and(eqId(id))
                .and(eqOrderId(orderId))
                .and(eqOrderDetailId(orderDetailId))
                .and(eqHubId(hubId))
                .and(eqCompanyId(companyId))
                .and(eqStatus(status))
                .and(eqManagerId(managerId))
                .and(eqIsDeleted(isDeleted));


        List<UUID> ids = queryFactory.select(qDelivery.id)
                .from(qDelivery)
                .where(builder)
                .orderBy(QueryDslUtils.getOrderSpecifiers(pageable.getSort(), Delivery.class))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 조회 쿼리
        List<Delivery> deliveries = queryFactory.selectDistinct(qDelivery)
                .from(qDelivery)
                .leftJoin(qDeliveryRoute).on(qDeliveryRoute.delivery.eq(qDelivery)).fetchJoin()
                .leftJoin(qDeliveryManager).on(qDelivery.manager.id.eq(qDeliveryManager.id)).fetchJoin()
                .where(qDelivery.id.in(ids))
                .orderBy(QueryDslUtils.getOrderSpecifiers(pageable.getSort(), Delivery.class))
                .fetch();


        // Count 쿼리
        Long total = queryFactory.select(qDelivery.count())
                .from(qDelivery)
                .leftJoin(qDeliveryRoute).on(qDeliveryRoute.delivery.eq(qDelivery))
                .leftJoin(qDeliveryManager).on(qDelivery.manager.id.eq(qDeliveryManager.id))
                .where(builder)
                .fetchOne();

        return new PageImpl<>(
                deliveries,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                total != null ? total : 0L);

    }

    private BooleanExpression eqId(UUID id) {
        if (id == null) {
            return null;
        }
        return qDelivery.id.eq(id);
    }

    private BooleanExpression eqOrderId(UUID orderId) {
        if (orderId == null) {
            return null;
        }
        return qDelivery.orderId.eq(orderId);
    }

    private BooleanExpression eqOrderDetailId(UUID orderDetailId) {
        if (orderDetailId == null) {
            return null;
        }
        return qDelivery.orderDetailId.eq(orderDetailId);
    }

    private BooleanExpression eqHubId(UUID hubId) {
        if (hubId == null) {
            return null;
        }
        return qDelivery.departureHubId.eq(hubId);
    }

    private BooleanExpression eqCompanyId(UUID companyId) {
        if (companyId == null) {
            return null;
        }
        return qDelivery.companyId.eq(companyId);
    }

    private BooleanExpression eqStatus(DeliveryStatus status) {
        if (status == null) {
            return null;
        }
        return qDelivery.status.eq(status);
    }

    private BooleanExpression eqManagerId(Long managerId) {
        if (managerId == null) {
            return null;
        }
        return qDelivery.manager.managerId.eq(managerId);
    }

    private BooleanExpression eqIsDeleted(Boolean isDeleted) {
        if (isDeleted == null) {
            return null;
        }
        return qDelivery.isDeleted.eq(isDeleted);
    }


    /**
     * 배송 경로 검색
     * @param criteria 검색 조건
     * @param pageable customPageable
     * @return 배송 경로
     */
    @Override
    public Page<DeliveryRoute> searchRoute(DeliveryRouteSearchCriteria criteria,
                                           Pageable pageable) {

        UUID routeId = criteria.getRouteId();
        UUID orderId = criteria.getOrderId();
        UUID orderDetailId = criteria.getOrderDetailId();
        UUID deliveryId = criteria.getDeliveryId();
        UUID departureHubId = criteria.getDepartureHubId();
        UUID arriveHubId = criteria.getArriveHubId();
        UUID companyId = criteria.getCompanyId();
        Long managerId = criteria.getManagerId();
        DeliveryRouteStatus status = criteria.getStatus();
        Boolean isDeleted = criteria.getIsDeleted();


        if (routeId != null) {
            builder.and(qDeliveryRoute.id.eq(routeId));
        }

        if (orderId != null) {
            builder.and(qDeliveryRoute.delivery.orderId.eq(orderId));
        }

        if (orderDetailId != null) {
            builder.and(qDeliveryRoute.delivery.orderDetailId.eq(orderDetailId));
        }

        if (deliveryId != null) {
            builder.and(qDelivery.id.eq(deliveryId));
        }

        if (departureHubId != null) {
            builder.and(qDeliveryRoute.departureHubId.eq(departureHubId));
        }

        if (arriveHubId != null) {
            builder.and(qDeliveryRoute.arriveHubId.eq(arriveHubId));
        }

        if (companyId != null) {
            builder.and(qDeliveryRoute.delivery.companyId.eq(companyId));
        }

        if (managerId != null) {
            builder.and(qDeliveryRoute.manager.managerId.eq(managerId));
        }

        if (status != null) {
            builder.and(qDeliveryRoute.status.eq(status));
        }

        if (isDeleted != null) {
            builder.and(qDeliveryRoute.isDeleted.eq(isDeleted));
        }

        List<UUID> routeIds = queryFactory.select(qDeliveryRoute.id)
                .from(qDeliveryRoute)
                .where(builder)
                .orderBy(qDeliveryRoute.sequence.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        // 조회 쿼리
        List<DeliveryRoute> routes = queryFactory.selectDistinct(qDeliveryRoute)
                .from(qDeliveryRoute)
                .leftJoin(qDelivery).on(qDeliveryRoute.delivery.eq(qDelivery)).fetchJoin()
                .leftJoin(qDeliveryManager).on(qDelivery.manager.id.eq(qDeliveryManager.id)).fetchJoin()
                .where(qDeliveryRoute.id.in(routeIds))
                .orderBy(qDeliveryRoute.sequence.asc())
                .fetch();


        // Count 쿼리
        Long total = queryFactory.select(qDeliveryRoute.count())
                .from(qDeliveryRoute)
                .leftJoin(qDelivery).on(qDeliveryRoute.delivery.eq(qDelivery))
                .leftJoin(qDeliveryManager).on(qDelivery.manager.id.eq(qDeliveryManager.id))
                .where(builder)
                .fetchOne();


        return new PageImpl<>(
                routes,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                total != null ? total : 0L);

    }

    /**
     * 배송 경로 조회 (admin)
     * @param routeId 배송 경로 id
     */
    @Override
    public Optional<DeliveryRoute> findRouteById(UUID routeId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(qDeliveryRoute)
                        .where(
                                qDeliveryRoute.id.eq(routeId)
                        )
                        .fetchOne()
        );
    }

    /**
     * 배송 경로 조회
     * @param routeId 배송 경로 id
     */
    @Override
    public Optional<DeliveryRoute> findRouteByIdAndIsDeletedFalse(UUID routeId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(qDeliveryRoute)
                        .where(
                                qDeliveryRoute.id.eq(routeId),
                                qDeliveryRoute.isDeleted.eq(false)
                        )
                        .fetchOne()
        );
    }

    @Override
    public Page<DeliveryManager> searchManager(DeliveryManagerSearchCriteria criteria,
                                               Pageable pageable) {
        UUID id = criteria.getId();
        String slackId = criteria.getSlackId();
        UUID hubId = criteria.getHubId();
        Long managerId = criteria.getManagerId();
        DeliveryManagerType type = criteria.getType();
        Integer sequence = criteria.getSequence();
        Boolean isDeleted = criteria.getIsDeleted();


        if (id != null) {
            builder.and(qDeliveryManager.id.eq(id));
        }

        if (slackId != null) {
            builder.and(qDeliveryManager.slackId.eq(slackId));
        }

        if (hubId != null) {
            builder.and(qDeliveryManager.hubId.eq(hubId));
        }

        if (managerId != null) {
            builder.and(qDeliveryManager.managerId.eq(managerId));
        }

        if (type != null) {
            builder.and(qDeliveryManager.type.eq(type));
        }

        if (sequence != null) {
            builder.and(qDeliveryManager.sequence.eq(sequence));
        }

        if (isDeleted != null) {
            builder.and(qDeliveryManager.isDeleted.eq(isDeleted));
        }

        // 조회 쿼리
        List<DeliveryManager> managers = queryFactory.selectDistinct(qDeliveryManager)
                .from(qDeliveryManager)
                .where(builder)
                .orderBy(qDeliveryManager.sequence.asc())
//                .orderBy(QueryDslUtils.getOrderSpecifiers(pageable.getSort(), DeliveryManager.class))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        // Count 쿼리
        Long total = queryFactory.select(qDeliveryManager.id.count())
                .from(qDeliveryManager)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(
                managers,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                total != null ? total : 0L);


    }

}
