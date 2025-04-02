package com.oringmaryho.business.slackservice.application.service;

import com.oringmaryho.business.slackservice.application.dto.mapper.SlackApplicationMapper;
import com.oringmaryho.business.slackservice.application.dto.request.SlackAdminMessageCreateRequestServiceDto;
import com.oringmaryho.business.slackservice.application.dto.request.SlackMessageDeleteRequestServiceDto;
import com.oringmaryho.business.slackservice.application.dto.request.SlackMessageFindRequestServiceDto;
import com.oringmaryho.business.slackservice.application.dto.request.SlackMessageSearchRequestServiceDto;
import com.oringmaryho.business.slackservice.application.dto.request.SlackMessageUpdateRequestServiceDto;
import com.oringmaryho.business.slackservice.application.feign.UserClient;
import com.oringmaryho.business.slackservice.application.utils.DirectMessageService;
import com.oringmaryho.business.slackservice.domain.SlackMessage;
import com.oringmaryho.business.slackservice.domain.SlackMessageSearchCriteria;
import com.oringmaryho.business.slackservice.domain.repository.CustomSlackMessageRepository;
import com.oringmaryho.business.slackservice.exception.ErrorCode;
import com.oringmaryho.business.slackservice.exception.SlackException;
import com.oringmaryho.business.slackservice.infrastructure.SlackJpaRepository;
import com.oringmaryho.business.slackservice.presentation.dto.request.SlackMessageUpdateResponseDto;
import com.oringmaryho.business.slackservice.presentation.dto.response.SlackMessageResponseDto;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackAdminMessageService {

  private final DirectMessageService directMessageService;
  private final UserClient userClient;
  private final SlackJpaRepository slackJpaRepository;
  private final SlackApplicationMapper slackApplicationMapper;
  private final CustomSlackMessageRepository customSlackMessageRepository;

  @Description("모든 슬랙 메시지 조회")
  @Transactional(readOnly = true)
  public Page<SlackMessageResponseDto> getSlackMessages(
      SlackMessageSearchRequestServiceDto requestServiceDto,
      Pageable pageable) {

    Page<SlackMessage> messages = customSlackMessageRepository.findDynamicQuery(
        createSlackSearchCriteria(requestServiceDto),
        pageable);

    return messages.map(slackApplicationMapper::toSlackMessageResponseDto);
  }

  public SlackMessageSearchCriteria createSlackSearchCriteria(
      SlackMessageSearchRequestServiceDto requestDto) {
    return SlackMessageSearchCriteria.builder()
        .id(requestDto.id())
        .receiverId(requestDto.receiverId())
        .message(requestDto.message())
        .sentAt(requestDto.sentAt())
        .isDeleted(requestDto.isDeleted())
        .build();
  }

  @Description("id로 슬랙 메시지 조회")
  @Transactional(readOnly = true)
  public SlackMessageResponseDto getSlackMessageById(
      SlackMessageFindRequestServiceDto requestServiceDto) {
    SlackMessage slackMessage = customSlackMessageRepository.findActiveSlackMessageById(
            requestServiceDto.id())
        .orElseThrow(() -> new SlackException(ErrorCode.NOT_FOUND));

    return slackApplicationMapper.toSlackMessageResponseDto(slackMessage);
  }

  @Description(
      "슬랙 메시지 생성: 슬랙 컨트롤러에서 받음"
  )
  @Transactional
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

  @Description("슬랙 메시지 수정")
  @Transactional
  public SlackMessageUpdateResponseDto updateSlackMessage(
      SlackMessageUpdateRequestServiceDto requestServiceDto) {
    SlackMessage slackMessage = slackJpaRepository.findById(requestServiceDto.id())
        .orElseThrow(() -> new SlackException(ErrorCode.NOT_FOUND));
    slackMessage.setMessage(requestServiceDto.message());
    return slackApplicationMapper.toSlackMessageUpdateResponseDto(requestServiceDto.id());
  }

  @Description("슬랙 메시지 삭제")
  @Transactional
  public void deleteSlackMessage(SlackMessageDeleteRequestServiceDto requestServiceDto) {
    SlackMessage slackMessage = slackJpaRepository.findById(requestServiceDto.id())
        .orElseThrow(() -> new SlackException(ErrorCode.NOT_FOUND));
    slackMessage.softDelete(requestServiceDto.userId());
  }
}
