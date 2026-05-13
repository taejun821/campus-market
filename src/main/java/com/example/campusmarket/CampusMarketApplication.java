package com.example.campusmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 캠퍼스 마켓 Spring Boot 애플리케이션 진입점
 * @SpringBootApplication이 컴포넌트 스캔, 자동 설정, 빈 등록을 모두 처리한다.
 */
@SpringBootApplication
public class CampusMarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusMarketApplication.class, args);
    }
}
