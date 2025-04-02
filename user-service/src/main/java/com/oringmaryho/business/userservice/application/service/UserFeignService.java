package com.oringmaryho.business.userservice.application.service;

import com.oringmaryho.business.userservice.application.dto.request.UserFromDeliveryRequestServiceDto;
import com.oringmaryho.business.userservice.application.dto.request.UsersRequestServiceDto;
import com.oringmaryho.business.userservice.domain.User;
import com.oringmaryho.business.userservice.domain.UserRoleType;
import com.oringmaryho.business.userservice.domain.repository.CustomUserRepository;
import com.oringmaryho.business.userservice.domain.repository.UserRepository;
import com.oringmaryho.business.userservice.exception.ErrorCode;
import com.oringmaryho.business.userservice.exception.UserException;
import com.oringmaryho.business.userservice.presentation.dto.request.SlackGetSlackIdRequestServiceDto;
import com.oringmaryho.business.userservice.presentation.dto.request.UserFromDeliveryGetRoleRequestServiceDto;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFeignService {

  private final CustomUserRepository customUserRepository;
  private final UserRepository userRepository;

  @Description(
      "권한과 삭제 여부를 받아(삭제 여부는 선택) 권한의 모든 사용자를 검색"
  )
  public List<User> userServiceGetByRole(UsersRequestServiceDto usersRequestServiceDto) {
    return customUserRepository.findUsersByRole(usersRequestServiceDto.role(),
            usersRequestServiceDto.isDeleted())
        .orElseThrow(() -> new UserException(ErrorCode.ROLE_ACTIVE_USERS_NOT_FOUND));
  }

  @Description(
      "배송 서비스에서 사용할 HUB_DELIVERY_MANAGER, COMPANY_DELIVERY_MANAGER 리스트 반환하는 메서드"
  )
  public Map<UserRoleType, List<User>> userServiceToDeliveryServiceMap(
      UserFromDeliveryRequestServiceDto requestServiceDto) {
    return Stream.of(UserRoleType.HUB_DELIVERY_MANAGER, UserRoleType.COMPANY_DELIVERY_MANAGER)
        .collect(Collectors.toMap(
            role -> role,
            role -> customUserRepository.findUsersByRole(role, requestServiceDto.isDeleted())
                .orElseThrow(() -> new UserException(ErrorCode.ROLE_ACTIVE_USERS_NOT_FOUND))
        ));
  }

  @Description(
      "배송 서비스에서 user id를 받아 해당하는 유저의 role을 반환하는 메서드"
  )
  public UserRoleType userFeignServiceGetRole(
      UserFromDeliveryGetRoleRequestServiceDto requestServiceDto) {
    User user = userRepository.findById(requestServiceDto.id())
        .orElseThrow(() -> new UserException(ErrorCode.NOT_FOUND));

    return user.getRole();
  }

  @Description(
      "슬랙 서비스에서 user id를 받아 해당하는 유저의 slackId를 반환하는 메서드"
  )
  public String userFeignServiceGetSlackId(SlackGetSlackIdRequestServiceDto requestServiceDto) {
    User user = userRepository.findById(requestServiceDto.id())
        .orElseThrow(() -> new UserException(ErrorCode.NOT_FOUND));
    return user.getSlackId();
  }

  @Transactional(readOnly = true)
  public String userFeignServiceGetUsername(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new UserException(ErrorCode.NOT_FOUND));

    return user.getUsername();
  }
}
