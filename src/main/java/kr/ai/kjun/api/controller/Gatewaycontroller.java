package kr.ai.kjun.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway 상태 점검 컨트롤러
 * 시스템 연결 상태를 확인하는 엔드포인트 제공
 */
@RestController
@RequestMapping("/api/gateway")
@Tag(name = "Gateway", description = "Gateway 상태 점검 API")
public class Gatewaycontroller {

    private final RedisTemplate<String, String> redisTemplate;
    private final DataSource dataSource;

    @Autowired
    public Gatewaycontroller(
            RedisTemplate<String, String> redisTemplate,
            DataSource dataSource) {
        this.redisTemplate = redisTemplate;
        this.dataSource = dataSource;
    }

    /**
     * Gateway 연결 상태 점검
     * GET /api/gateway/status
     * 
     * Redis, PostgreSQL 데이터베이스 연결 상태를 확인합니다.
     */
    @GetMapping("/status")
    @Operation(summary = "Gateway 상태 점검", description = "Redis와 PostgreSQL 데이터베이스 연결 상태를 확인합니다.")
    public ResponseEntity<Map<String, Object>> checkStatus() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> services = new HashMap<>();

        boolean allHealthy = true;

        // Redis 연결 상태 확인
        Map<String, Object> redisStatus = checkRedisConnection();
        services.put("redis", redisStatus);
        if (!(Boolean) redisStatus.get("connected")) {
            allHealthy = false;
        }

        // PostgreSQL 데이터베이스 연결 상태 확인
        Map<String, Object> dbStatus = checkDatabaseConnection();
        services.put("database", dbStatus);
        if (!(Boolean) dbStatus.get("connected")) {
            allHealthy = false;
        }

        response.put("status", allHealthy ? "healthy" : "unhealthy");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("services", services);

        return allHealthy
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(503).body(response);
    }

    /**
     * Redis 연결 상태 확인
     */
    private Map<String, Object> checkRedisConnection() {
        Map<String, Object> status = new HashMap<>();
        try {
            // Redis ping 테스트
            var connection = redisTemplate.getConnectionFactory().getConnection();
            try {
                String result = connection.ping();
                status.put("connected", true);
                status.put("message", "Redis 연결 성공");
                status.put("ping", result);
            } finally {
                // 연결 닫기
                if (connection != null) {
                    connection.close();
                }
            }
        } catch (Exception e) {
            status.put("connected", false);
            status.put("message", "Redis 연결 실패: " + e.getMessage());
            status.put("error", e.getClass().getSimpleName());
        }
        return status;
    }

    /**
     * PostgreSQL 데이터베이스 연결 상태 확인
     */
    private Map<String, Object> checkDatabaseConnection() {
        Map<String, Object> status = new HashMap<>();
        try {
            // 데이터베이스 연결 테스트
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(2); // 2초 타임아웃

                if (isValid) {
                    status.put("connected", true);
                    status.put("message", "PostgreSQL 연결 성공");
                    status.put("database", connection.getCatalog());
                    status.put("url", connection.getMetaData().getURL());
                } else {
                    status.put("connected", false);
                    status.put("message", "PostgreSQL 연결 검증 실패");
                }
            }
        } catch (Exception e) {
            status.put("connected", false);
            status.put("message", "PostgreSQL 연결 실패: " + e.getMessage());
            status.put("error", e.getClass().getSimpleName());
        }
        return status;
    }
}
