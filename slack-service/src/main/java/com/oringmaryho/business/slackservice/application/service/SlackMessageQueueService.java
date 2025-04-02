package com.oringmaryho.business.slackservice.application.service;

import com.oringmaryho.business.slackservice.application.dto.request.SlackMessageDto;
import com.oringmaryho.business.slackservice.application.feign.UserClient;
import com.oringmaryho.business.slackservice.application.messaging.MessageHandler;
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
public class SlackMessageQueueService implements MessageHandler {

  private final UserClient userClient;
  private final DirectMessageService directMessageService;
  private final SlackJpaRepository slackJpaRepository;

  @Override
  public void handleMessage(SlackMessageDto slackMessageDto) {
    sendToSlack(slackMessageDto);
  }

  @Description(
      "메시지 큐에서 dto를 받아 메시지를 보내는 서비스"
  )
  private void sendToSlack(SlackMessageDto requestDto) {
    ResponseEntity<String> response = userClient.getUserSlackIdById(requestDto.id());
    String slackId = response.getBody();
    log.info("메시지 수신자의 slackId:{}", slackId);
    String message = requestDto.message();
    if (slackId == null) {
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
