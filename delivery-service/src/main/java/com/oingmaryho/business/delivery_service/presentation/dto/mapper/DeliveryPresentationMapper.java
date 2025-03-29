package com.oingmaryho.business.delivery_service.presentation.dto.mapper;

import com.oingmaryho.business.delivery_service.application.dto.request.*;
import com.oingmaryho.business.delivery_service.application.dto.response.*;
import com.oingmaryho.business.delivery_service.presentation.dto.request.*;
import com.oingmaryho.business.delivery_service.presentation.dto.response.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface DeliveryPresentationMapper {

    // RequestServiceDto -> RequestDto
    @Mapping(target = "receiver", source = "requestDto.requesterName")
    @Mapping(target = "receiverSlackId", source = "requestDto.requesterSlackId")
    @Mapping(target = "companyId", source = "requestDto.recipientId")
    @Mapping(target = "hubId", source = "requestDto.recipientHubId")
    @Mapping(target = "address", source = "requestDto.requesterAddress")
    DeliveryCreationRequestServiceDto toCreationServiceDto(DeliveryCreationRequestDto requestDto);

    @Mapping(target = "id", source = "id")
    DeliveryUpdateRequestServiceDto toUpdateServiceDto(UUID id, DeliveryUpdateRequestDto requestDto);

    @Mapping(target = "id", source = "id")
    DeliveryUpdateStatusRequestServiceDto toUpdateStatusServiceDto(UUID id, DeliveryUpdateStatusRequestDto requestDto);

    DeliveryDeletionRequestServiceDto toDeletionServiceDto(UUID id);

    DeliveryDetailRequestServiceDto toDetailServiceDto(UUID id);

    @Mapping(target = "customPageable", source = "customPageable")
    DeliverySearchRequestServiceDto toSearchServiceDto(DeliverySearchRequestDto requestDto, Pageable customPageable);

    DeliveryRouteDetailRequestServiceDto toRouteDetailServiceDto(UUID id);

    @Mapping(target = "deliveryId", source = "id")
    @Mapping(target = "customPageable", source = "customPageable")
    DeliveryRouteSearchRequestServiceDto toRouteSearchServiceDto(UUID id,DeliveryRouteSearchRequestDto searchDto, Pageable customPageable);

    @Mapping(target = "id", source = "id")
    DeliveryRouteUpdateStatusRequestServiceDto toUpdateRouteStatusServiceDto(UUID id, DeliveryRouteUpdateStatusRequestDto requestDto);

    DeliveryManagerDetailRequestServiceDto toManagerDetailRequestDto(UUID id);

    @Mapping(target = "customPageable", source = "customPageable")
    DeliveryManagerSearchRequestServiceDto toManagerSearchRequestDto(DeliveryManagerSearchRequestDto requestDto, Pageable customPageable);

    // ResponseServiceDto -> ResponseDto
    DeliveryUpdateResponseDto toUpdateResponseDto(DeliveryUpdateResponseServiceDto responseServiceDto);

    DeliveryUpdateStatusResponseDto toUpdateStatusResponseDto(DeliveryUpdateStatusResponseServiceDto responseServiceDto);

    // 배송 조회
    DeliveryResponseDto toDetailResponseDto(DeliveryResponseServiceDto responseServiceDto);

    // 배송 검색
    DeliveryResponseDto toSearchResponseDto(DeliveryResponseServiceDto responseServiceDto);


    // 배송 경로 조회
    DeliveryRouteResponseDto toRouteDetailResponseDto(DeliveryRouteResponseServiceDto responseServiceDto);

    // 배송 경로 검색
    DeliveryRouteResponseDto toRouteSearchResponseDto(DeliveryRouteResponseServiceDto responseServiceDto);

    // 배송 조회 (admin)
    DeliveryAdminResponseDto toDetailAdminResponseDto(DeliveryResponseServiceDto responseServiceDto);

    // 배송 검색 (admin)
    DeliveryAdminResponseDto toSearchAdminResponseDto(DeliveryResponseServiceDto responseServiceDto);

    // 배송 경로 조회 (admin)
    DeliveryRouteAdminResponseDto toRouteDetailAdminResponseDto(DeliveryRouteResponseServiceDto responseServiceDto);

    // 배송 경로 검색 (admin)
    DeliveryRouteAdminResponseDto toRouteSearchAdminResponseDto(DeliveryRouteResponseServiceDto responseServiceDto);

    DeliveryRouteUpdateStatusResponseDto toUpdateRouteStatusResponseDto(DeliveryRouteUpdateStatusResponseServiceDto responseServiceDto);

    // 배송 담당자 검색
    DeliveryManagerResponseDto toMangerResponseDto(DeliveryManagerResponseServiceDto responseServiceDto);

    // 배송 담당자 검색 (admin)
    DeliveryManagerAdminResponseDto toMangerAdminResponseDto(DeliveryManagerResponseServiceDto responseServiceDto);

}
