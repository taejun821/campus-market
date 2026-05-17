package com.example.campusmarket.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 채팅 메시지 전송 요청 DTO
 * 발신자 정보는 JWT 토큰에서 추출하므로 내용(content)만 전달한다.
 */
public record MessageRequest(
    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 1000, message = "메시지는 1000자 이하여야 합니다.") String content
) {}
