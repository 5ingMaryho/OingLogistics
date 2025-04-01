package com.oingmaryho.business.delivery_service.application.dto.context;

import com.oingmaryho.business.delivery_service.domain.entity.Delivery;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CompanyAssignmentContext {
    private int backupSequence;
    private String sequenceKey;
    private Delivery delivery;
    private UUID arriveHubId;
}
