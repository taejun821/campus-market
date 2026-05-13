package com.example.campusmarket.common.exception;

/**
 * 클라이언트의 잘못된 요청을 나타내는 예외 (HTTP 400 Bad Request)
 * 예: 허용되지 않은 이메일 도메인, 비밀번호 불일치
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
