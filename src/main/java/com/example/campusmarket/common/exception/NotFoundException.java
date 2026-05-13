package com.example.campusmarket.common.exception;

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 예외 (HTTP 404 Not Found)
 * 예: 존재하지 않는 게시물·분실물 ID, 미가입 이메일로 로그인 시도
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
