package com.oringmaryho.business.userservice.application.service;

import com.oringmaryho.business.userservice.application.UserHelper;
import com.oringmaryho.business.userservice.application.dto.mapper.UserApplicationMapper;
import com.oringmaryho.business.userservice.application.dto.request.UserSearchRequestServiceDto;
import com.oringmaryho.business.userservice.application.dto.request.UserSignInRequestServiceDto;
import com.oringmaryho.business.userservice.application.dto.request.UserSignOutRequestServiceDto;
import com.oringmaryho.business.userservice.application.dto.request.UserSignUpRequestServiceDto;
import com.oringmaryho.business.userservice.application.dto.request.UserSlackCodeRequestServiceDto;
import com.oringmaryho.business.userservice.application.dto.request.UserSlackConfirmRequestServiceDto;
import com.oringmaryho.business.userservice.application.dto.response.UserSignInResponseServiceDto;
import com.oringmaryho.business.userservice.application.messaging.UserMessageService;
import com.oringmaryho.business.userservice.application.utils.CodeStorage;
import com.oringmaryho.business.userservice.application.utils.DirectMessageAuthService;
import com.oringmaryho.business.userservice.application.utils.RedisUtil;
import com.oringmaryho.business.userservice.application.utils.jwt.JwtTokenProvider;
import com.oringmaryho.business.userservice.domain.User;
import com.oringmaryho.business.userservice.domain.UserConfirmStatus;
import com.oringmaryho.business.userservice.domain.repository.UserRepository;
import com.oringmaryho.business.userservice.exception.ErrorCode;
import com.oringmaryho.business.userservice.exception.UserException;
import com.oringmaryho.business.userservice.presentation.dto.response.UserSearchResponseDto;
import com.oringmaryho.business.userservice.presentation.dto.response.UserSignInResponseDto;
import io.jsonwebtoken.Claims;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserApplicationMapper userApplicationMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final DirectMessageAuthService directMessageAuthService;
  private final UserMessageService userMessageService;
  private final RedisUtil redisUtil;
  private final RedisTemplate<String, Object> redisTemplate;
  private final CodeStorage codeStorage;
  private final UserHelper userHelper;

  @Value("${slack.code.ttl}")
  private Long SLACK_CODE_TTL;

  @Transactional
  public void signUpUser(UserSignUpRequestServiceDto requestServiceDto) {
    userHelper.validateRequiredField(requestServiceDto.username(), ErrorCode.USERNAME_NULL);
    userHelper.validateRequiredField(requestServiceDto.password(), ErrorCode.PASSWORD_NULL);
    userHelper.validateRequiredField(requestServiceDto.slackId(), ErrorCode.SLACKID_NULL);

    userHelper.usernameVerify(requestServiceDto.username());
    userHelper.passwordVerify(requestServiceDto.password());
    userHelper.checkUsernameExists(requestServiceDto.username(), userRepository);

    String encodedPassword = userHelper.encodePassword(requestServiceDto.password(),
        passwordEncoder);
    User user = User.builder()
        .username(requestServiceDto.username())
        .password(encodedPassword)
        .slackId(requestServiceDto.slackId())
        .build();

    userRepository.save(user);
  }

  public UserSignInResponseDto signInUser(UserSignInRequestServiceDto requestServiceDto) {
    if (!userRepository.existsByUsername(requestServiceDto.username())) {
      throw new UserException(ErrorCode.NOT_FOUND);
    }

    User user = userHelper.findUserByUsername(requestServiceDto.username(), userRepository);
    redisUtil.updateUserInfo(user);
    Map<String, String> tokenMap = redisUtil.updateUserJwtToken(user.getId());

    UserSignInResponseServiceDto serviceDto = new UserSignInResponseServiceDto(
        tokenMap.get("accessToken"),
        tokenMap.get("refreshToken")
    );
    return userApplicationMapper.toSignInResponseDto(serviceDto);
  }

  public void signOutUser(UserSignOutRequestServiceDto requestServiceDto) {
    Long userId = requestServiceDto.id();
    String tokenKey = "user:token:" + userId;
    if (!redisTemplate.hasKey(tokenKey)) {
      throw new UserException(ErrorCode.NOT_FOUND);
    }
    Map<Object, Object> token = redisTemplate.opsForHash().entries(tokenKey);
    String accessToken = (String) token.get("accessToken");

    if (token == null || token.isEmpty()) {
      throw new UserException(ErrorCode.JWT_REQUIRED);
    }

    Claims claims = jwtTokenProvider.parseJwtToken(accessToken);
    long ttlMillis = jwtTokenProvider.calculateTtlMillis(claims.getExpiration());

    String blacklistKey = "blacklist:" + accessToken;
    redisTemplate.opsForValue().set(blacklistKey, "blacklisted");

    if (ttlMillis > 0) {
      redisTemplate.expire(blacklistKey, ttlMillis, TimeUnit.MILLISECONDS);
      log.debug("블랙리스트에 추가된 토큰 : {} TTL: {} ms", token, ttlMillis);
    } else {
      redisTemplate.expire(blacklistKey, 1, TimeUnit.SECONDS);
      log.debug("토큰 {} 은 이미 만료되어 최소 TTL로 설정", token);
    }

    if (userId != null) {
      String userInfoKey = "user:token:" + userId;
      redisTemplate.delete(userInfoKey);
    }
  }

  public UserSearchResponseDto searchUser(UserSearchRequestServiceDto requestServiceDto) {
    if (!requestServiceDto.id().equals(requestServiceDto.userId())) {
      throw new UserException(ErrorCode.USER_NOT_MATCH);
    }

    User user = userHelper.findUserById(requestServiceDto.id(), userRepository);
    return userApplicationMapper.toUserSearchResponseDto(user);
  }

  public void slackCodeRequestUser(UserSlackCodeRequestServiceDto requestServiceDto) {
    User user = userHelper.findUserByUsername(requestServiceDto.username(), userRepository);
    userHelper.validateRequiredField(user.getUsername(), ErrorCode.USERNAME_NULL);
    userHelper.validateRequiredField(user.getSlackId(), ErrorCode.SLACKID_NULL);

    if (user.getStatus().equals(UserConfirmStatus.CONFIRMED)) {
      throw new UserException(ErrorCode.SLACK_ALREADY_AUTH);
    }

    String slackCode = directMessageAuthService.generateCode();
    String slackMessage = directMessageAuthService.makeDirectMessage(slackCode);
    userMessageService.sendSlackMessage(user.getId(), slackMessage);

    if (codeStorage.hasKey(requestServiceDto.username())) {
      codeStorage.removeCode(requestServiceDto.username());
    }
    codeStorage.storeCode(requestServiceDto.username(), user.getSlackId(), slackCode,
        SLACK_CODE_TTL);
  }

  @Transactional
  public void slackConfirmUser(UserSlackConfirmRequestServiceDto requestServiceDto) {
    String username = requestServiceDto.username();
    String slackId = requestServiceDto.slackId();
    String slackCode = codeStorage.getCode(username);

    if (slackCode != null
        && codeStorage.getSlackUsername(username).equals(slackId)
        && slackCode.equals(requestServiceDto.confirmCode())) {
      User user = userHelper.findUserByUsername(username, userRepository);
      user.changeStatus(UserConfirmStatus.CONFIRMED);
      codeStorage.removeCode(username);
    } else {
      throw new UserException(ErrorCode.SLACK_AUTH_FAIL);
    }

    User updatedUser = userHelper.findUserByUsername(username, userRepository);
    redisUtil.updateUserInfo(updatedUser);
  }
}