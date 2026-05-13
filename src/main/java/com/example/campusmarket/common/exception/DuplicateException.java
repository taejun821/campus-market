package com.example.campusmarket.common.exception;

/**
 * 이미 존재하는 데이터를 중복 등록하려 할 때 발생하는 예외 (HTTP 409 Conflict)
 * 예: 동일한 이메일 주소로 재가입 시도
 */
public class DuplicateException extends RuntimeException {
    public DuplicateException(String message) {
        super(message);
    }
}
