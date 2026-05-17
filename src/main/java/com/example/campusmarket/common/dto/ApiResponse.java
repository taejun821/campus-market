package com.example.campusmarket.common.dto;

/**
 * 모든 API 응답에 사용되는 공통 래퍼 레코드
 *
 * 성공 응답: { success: true,  data: {...},  message: null    }
 * 오류 응답: { success: false, data: null,   message: "오류..." }
 *
 * @param success 요청 처리 성공 여부
 * @param data    응답 데이터 (오류 시 null)
 * @param message 오류 메시지 (성공 시 null)
 */
public record ApiResponse<T>(boolean success, T data, String message) {

    // 정상 처리 시 data를 감싸 반환
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return success(data);
    }

    // 오류 발생 시 message를 감싸 반환
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
