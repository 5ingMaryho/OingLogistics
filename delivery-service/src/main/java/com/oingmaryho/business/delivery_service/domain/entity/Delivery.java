package com.oingmaryho.business.delivery_service.domain.entity;

import com.oingmaryho.business.common.domain.entity.BaseEntity;
import com.oingmaryho.business.delivery_service.domain.type.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Builder
@Table(name = "p_delivery")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;      // TODO 배송 생성 시 주문 id 유효성 검사

    @Column(nullable = false)
    private UUID orderDetailId;

    @Column(nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.HUB_WAITING;

    @Column(nullable = false)
    private UUID departureHubId;

    private String departureHubName;

    @Column(nullable = false)
    private UUID arriveHubId;

    private String arriveHubName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String receiver;

    @Column(nullable = false)
    private String receiverSlackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private DeliveryManager manager; // 업체 배송 담당자 userId

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeliveryRoute> routes = new ArrayList<>();

    public void update(String receiver, String receiverSlackId, String address, DeliveryManager manager) {
        if (receiver != null) {
            this.receiver = receiver;
        }
        if (receiverSlackId != null) {
            this.receiverSlackId = receiverSlackId;
        }
        if (address != null) {
            this.address = address;
        }
        if (manager != null) {
            this.manager = manager;
        }
    }

    public void updateStatus(DeliveryStatus newStatus) {
        this.status = newStatus;
    }

}
