package com.oingmaryho.business.delivery_service.application.dto.context;

import com.oingmaryho.business.delivery_service.domain.entity.DeliveryRoute;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class HubAssignmentContext {
    private int backupSequence;
    private String sequenceKey;
    private List<DeliveryRoute> hubRoutes;
    private int sequence;
}