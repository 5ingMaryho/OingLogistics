package com.oingmaryho.business.delivery_service.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    DELIVERY_NOT_FOUND("D-001", "해당 배송을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ROUTE_NOT_FOUND("D-002", "해당 배송 경로를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MANAGER_NOT_FOUND("D-003", "해당 배송 담당자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    UNAUTHORIZED("D-004", "해당 요청에 대한 권한이 없습니다.", HttpStatus.UNAUTHORIZED),
    BAD_REQUEST("D-005", "해당 요청은 잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("D-006","처리 중 문제가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    HUB_SERVICE_UNAVAILABLE("H-001", "허브 서비스를 이용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    HUB_NOT_FOUND("H-001", "허브를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    HUB_PATH_NOT_FOUND("H-002", "배송 경로를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMPANY_SERVICE_UNAVAILABLE("C-001", "업체 서비스를 이용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    COMPANY_NOT_FOUND("C-002", "해당 업체를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_SERVICE_UNAVAILABLE("U-001", "유저 서비스를 이용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    USER_ROLE_NOT_FOUND("U-002","해당 유저 권한을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_NAME_NOT_FOUND("U-003", "해당 유저 이름을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    DELIVERY_MANAGER_NOT_ASSIGNED("L-001", "배송 담당자를 배정하는 중 문제가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    LOCK_FAILED("L-002","락을 획득하는 중 문제가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    UNLOCK_FAILED("L-003","락을 해제하는 중 문제가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

}
