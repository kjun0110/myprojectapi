package kr.ai.kjun.api.services.oauthservice.oauth;

import kr.ai.kjun.api.services.oauthservice.jwt.JwtTokenProvider;
import kr.ai.kjun.api.services.oauthservice.jwt.RefreshTokenService;
import kr.ai.kjun.api.services.oauthservice.jwt.TokenBlacklistService;
import kr.ai.kjun.api.services.oauthservice.service.UserManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth 인증 관련 API 컨트롤러
 * OAuth 로그인 후 토큰 갱신, 로그아웃 등 처리
 * 
 * OAuth 로그인 후 필요한 기능:
 * - Refresh Token으로 Access Token 갱신
 * - 로그아웃 (토큰 무효화)
 */
@RestController
@RequestMapping("/oauth")
public class OAuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserManagementService userManagementService;

    public OAuthController(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            TokenBlacklistService tokenBlacklistService,
            UserManagementService userManagementService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userManagementService = userManagementService;
    }

    /**
     * Refresh Token으로 Access Token 갱신
     * POST /oauth/refresh
     * 
     * Request Body:
     * {
     * "refreshToken": "refresh_token_string",
     * "userId": 123
     * }
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, Object> request) {
        String refreshToken = (String) request.get("refreshToken");
        Long userId = null;

        // userId를 Long으로 변환
        if (request.get("userId") instanceof Integer) {
            userId = ((Integer) request.get("userId")).longValue();
        } else if (request.get("userId") instanceof Long) {
            userId = (Long) request.get("userId");
        } else if (request.get("userId") instanceof String) {
            try {
                userId = Long.parseLong((String) request.get("userId"));
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(buildErrorResponse("유효하지 않은 사용자 ID입니다"));
            }
        }

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse("Refresh Token이 필요합니다"));
        }

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse("사용자 ID가 필요합니다"));
        }

        // Refresh Token 검증
        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildErrorResponse("유효하지 않은 Refresh Token입니다"));
        }

        // 사용자 정보 조회 (User Service HTTP 호출)
        Map<String, Object> user;
        try {
            user = userManagementService.findById(userId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse("사용자를 찾을 수 없습니다"));
        }

        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateToken(
                userId,
                (String) user.get("email"),
                (String) user.get("nickname"));

        // Refresh Token 갱신 (선택적 - 보안 강화를 위해)
        String newRefreshToken = refreshTokenService.refreshToken(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", newAccessToken);
        response.put("refreshToken", newRefreshToken);

        System.out.println("✅ [토큰 갱신] 성공 - User ID: " + userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     * POST /oauth/logout
     * 
     * Request Body:
     * {
     * "userId": 123,
     * "accessToken": "jwt_token_string" // 선택적
     * }
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, Object> request) {
        Long userId = null;

        // userId를 Long으로 변환
        if (request.get("userId") instanceof Integer) {
            userId = ((Integer) request.get("userId")).longValue();
        } else if (request.get("userId") instanceof Long) {
            userId = (Long) request.get("userId");
        } else if (request.get("userId") instanceof String) {
            try {
                userId = Long.parseLong((String) request.get("userId"));
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(buildErrorResponse("유효하지 않은 사용자 ID입니다"));
            }
        }

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse("사용자 ID가 필요합니다"));
        }

        // Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(userId);

        // Access Token을 블랙리스트에 추가 (제공된 경우)
        String accessToken = (String) request.get("accessToken");
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            tokenBlacklistService.addToBlacklist(accessToken);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그아웃되었습니다");

        System.out.println("✅ [로그아웃] 완료 - User ID: " + userId);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        return errorResponse;
    }
}
