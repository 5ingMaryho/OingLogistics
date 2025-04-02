package com.oringmaryho.business.slackservice.application.service;

import com.oringmaryho.business.slackservice.application.dto.request.SlackAdminMessageCreateRequestServiceDto;
import com.oringmaryho.business.slackservice.application.feign.UserClient;
import com.oringmaryho.business.slackservice.application.utils.DirectMessageService;
import com.oringmaryho.business.slackservice.domain.SlackMessage;
import com.oringmaryho.business.slackservice.exception.ErrorCode;
import com.oringmaryho.business.slackservice.exception.SlackException;
import com.oringmaryho.business.slackservice.infrastructure.SlackJpaRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackMessageService {

  private final DirectMessageService directMessageService;
  private final UserClient userClient;
  private final SlackJpaRepository slackJpaRepository;

  @Description(
      "슬랙 메시지 생성: 슬랙 컨트롤러에서 받음"
  )
  public void createSlackMessage(SlackAdminMessageCreateRequestServiceDto requestDto) {

    ResponseEntity<String> response = userClient.getUserSlackIdById(requestDto.id());
    String slackId = response.getBody();

    String message = requestDto.message();
    if (slackId == null || slackId.isEmpty()) {
      throw new SlackException(ErrorCode.SLACK_ID_EMPTY);
    }
    directMessageService.sendDirectMessage(slackId, message);

    SlackMessage slackMessage = SlackMessage.builder()
        .receiverId(requestDto.id())
        .message(message)
        .sentAt(LocalDateTime.now())
        .build();
    slackJpaRepository.save(slackMessage);
  }

}
