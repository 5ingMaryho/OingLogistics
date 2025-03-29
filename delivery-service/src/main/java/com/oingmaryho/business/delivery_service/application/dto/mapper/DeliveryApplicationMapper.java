package com.oingmaryho.business.delivery_service.application.dto.mapper;

import com.oingmaryho.business.delivery_service.application.dto.request.DeliveryCreationRequestServiceDto;
import com.oingmaryho.business.delivery_service.application.dto.request.DeliveryManagerAssignmentRequestServiceDto;
import com.oingmaryho.business.delivery_service.application.dto.request.OrderMessageCreationRequestServiceDto;
import com.oingmaryho.business.delivery_service.application.dto.response.*;
import com.oingmaryho.business.delivery_service.domain.entity.Delivery;
import com.oingmaryho.business.delivery_service.domain.entity.DeliveryManager;
import com.oingmaryho.business.delivery_service.domain.entity.DeliveryRoute;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Mapper(componentModel = "spring")
public interface DeliveryApplicationMapper {

    // RequestServiceDto -> Entity
    @BeanMapping(ignoreByDefault = true)
    Delivery toDelivery(UUID managerId,
                        UUID departureHubId,
                        UUID destinationHubId,
                        DeliveryCreationRequestServiceDto requestServiceDto,
                        List<DeliveryRoute> routes);

    // Entity -> ResponseServiceDto
    DeliveryCreationResponseServiceDto toCreationResponseServiceDto(UUID orderId,
                                                                    UUID orderDetailId,
                                                                    UUID deliveryId,
                                                                    String deliveryDepartureName,
                                                                    String deliveryStopoverNames,
                                                                    String deliveryDestinationName,
                                                                    String deliveryManagerName,
                                                                    String deliveryManagerSlackId);

    DeliveryManagerAssignmentRequestServiceDto toManagerAssignmentRequestServiceDto(UUID deliveryId);
    OrderMessageCreationRequestServiceDto toOrderMessageCreationRequestServiceDto(UUID deliveryId);

    DeliveryUpdateResponseServiceDto toUpdateResponseServiceDto(UUID id);
    DeliveryUpdateStatusResponseServiceDto toUpdateStatusResponseServiceDto(UUID id);
    DeliveryRouteUpdateStatusResponseServiceDto toUpdateRouteStatusResponseServiceDto(UUID id);
    // 배송 정보
    @Mapping(target = "managerId", source = "delivery.manager.id")
    DeliveryResponseServiceDto toDeliveryResponseServiceDto(Delivery delivery);

    // 배송 경로 정보
    @Mapping(target = "deliveryId", source = "route.delivery.id")
    @Mapping(target = "managerId", source = "delivery.manager.id")
    DeliveryRouteResponseServiceDto toRouteResponseServiceDto(DeliveryRoute route);

    // 배송 담당자 정보
    DeliveryManagerResponseServiceDto toManagerResponseServiceDto(DeliveryManager manager);



}
