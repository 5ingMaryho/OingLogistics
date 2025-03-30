package com.oingmaryho.business.delivery_service.domain.entity;

import com.oingmaryho.business.common.domain.entity.BaseEntity;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryRouteStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Entity
@Builder
@Table(name = "p_delivery_route")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class DeliveryRoute extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    private Integer sequence;

    @Column(nullable = false)
    private UUID departureHubId;

    @Column(nullable = false)
    private String departureHubName;

    @Column(nullable = false)
    private UUID arriveHubId;

    @Column(nullable = false)
    private String arriveHubName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryRouteStatus status = DeliveryRouteStatus.HUB_WAITING;

    @Column(nullable = false)
    private Double estimatedDistance;

    @Column(nullable = false)
    private Integer estimatedTime;

    private Double actualDistance;
    private Integer actualTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private DeliveryManager manager; // 허브 배송 담당자 id

    public void addRoute(Delivery delivery) {
        delivery.getRoutes().add(this);
    }

    /**
     * 배송 상태 변경
     * @param newStatus 변경할 상태
     */
    public void changeStatus(DeliveryRouteStatus newStatus){
        this.status = newStatus;
    }

    /**
     * 허브 배송 담당자 배정
     * @param manager 허브 배송 담당자
     */
    public void assignManager(int sequence, DeliveryManager manager) {
        this.sequence = sequence;
        this.manager = manager;
    }

    /**
     * 배송 실제 거리, 실제 소요시간 입력
     * @param actualDistance 실제 거리
     * @param actualTime 실제 소요시간
     */
    public void updateActualDistanceAndTime(double actualDistance, int actualTime) {
        this.actualDistance = actualDistance;
        this.actualTime = actualTime;
    }
}
