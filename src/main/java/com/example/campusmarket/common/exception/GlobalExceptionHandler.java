package com.example.campusmarket.common.exception;

import com.example.campusmarket.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리기
 *
 * 모든 컨트롤러에서 발생하는 예외를 일관된 ApiResponse 형식으로 변환한다.
 * 예외 종류별 HTTP 상태 코드 매핑:
 * - BadRequestException             → 400 Bad Request
 * - NotFoundException               → 404 Not Found
 * - DuplicateException              → 409 Conflict
 * - ForbiddenException              → 403 Forbidden
 * - MethodArgumentNotValidException → 400 Bad Request (Bean Validation 실패)
 * - Exception (그 외 모든 예외)       → 500 Internal Server Error
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 잘못된 요청 처리 (예: 허용되지 않은 도메인, 비밀번호 불일치)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    // 리소스 미존재 (예: 없는 게시물 ID, 미가입 이메일)
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
    }

    // 중복 데이터 충돌 (예: 이미 가입된 이메일)
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
    }

    // 권한 없음 (예: 타인의 게시물 삭제 시도)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
    }

    // @Valid 유효성 검증 실패 → 필드별 오류 메시지를 쉼표로 연결해 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    // 예상치 못한 서버 내부 오류 (상세 정보는 서버 로그로만 확인)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("서버 오류가 발생했습니다."));
    }
}
