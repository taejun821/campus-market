package com.example.campusmarket.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 중고거래 상태 변경 요청 DTO
 *
 * @param status 변경할 거래 상태 (허용값: SELLING·RESERVED·SOLD)
 *               - SELLING  : 판매중
 *               - RESERVED : 예약중
 *               - SOLD     : 판매완료
 */
public record TradeStatusRequest(
    @NotBlank
    @Pattern(regexp = "SELLING|RESERVED|SOLD", message = "status는 SELLING, RESERVED, SOLD 중 하나여야 합니다.")
    String status
) {}
