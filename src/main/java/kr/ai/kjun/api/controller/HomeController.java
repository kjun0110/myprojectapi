package kr.ai.kjun.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 홈 컨트롤러
 * 기본 헬스체크 및 서비스 정보 제공
 */
@RestController
@Tag(name = "Home", description = "홈 API")
public class HomeController {

    /**
     * 루트 경로 헬스체크
     * GET /
     * 
     * 서비스가 실행 중인지 확인하는 간단한 헬스체크 엔드포인트
     * 로드밸런서나 모니터링 도구에서 사용 가능
     */
    @GetMapping("/")
    @Operation(summary = "루트 경로 헬스체크", description = "서비스가 실행 중인지 확인합니다.")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "api-service");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "API Service is running");
        return ResponseEntity.ok(response);
    }
}
