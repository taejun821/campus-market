package com.example.campusmarket.common.exception;

/**
 * 인증은 되었으나 해당 리소스에 대한 권한이 없을 때 발생하는 예외 (HTTP 403 Forbidden)
 * 예: 타인이 등록한 게시물·분실물을 삭제하려는 시도
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
