package kr.ai.kjun.api.services.oauthservice.naver;

import kr.ai.kjun.api.services.oauthservice.jwt.JwtTokenProvider;
import kr.ai.kjun.api.services.oauthservice.jwt.RefreshTokenService;
import kr.ai.kjun.api.services.oauthservice.service.UserManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth/naver")
public class NaverController {

    private final NaverService naverService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserManagementService userManagementService;
    private final RefreshTokenService refreshTokenService;
    private final kr.ai.kjun.api.services.oauthservice.jwt.AccessTokenService accessTokenService;

    @Value("${FRONT_LOGIN_CALLBACK_URL}")
    private String frontendLoginCallbackUrl;

    public NaverController(
            NaverService naverService,
            JwtTokenProvider jwtTokenProvider,
            UserManagementService userManagementService,
            RefreshTokenService refreshTokenService,
            kr.ai.kjun.api.services.oauthservice.jwt.AccessTokenService accessTokenService) {
        this.naverService = naverService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userManagementService = userManagementService;
        this.refreshTokenService = refreshTokenService;
        this.accessTokenService = accessTokenService;
    }

    // 네이버 로그인 URL 반환
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> getNaverLoginUrl() {
        String loginUrl = naverService.getNaverLoginUrl();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("loginUrl", loginUrl);

        System.out.println("🔗 [네이버 로그인] 로그인 URL 생성: " + loginUrl);
        return ResponseEntity.ok(response);
    }

    // 네이버 OAuth 콜백 처리
    @GetMapping("/callback")
    public ResponseEntity<?> naverCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        return handleNaverCallback(code, state, error);
    }

    private ResponseEntity<?> handleNaverCallback(String code, String state, String error) {
        System.out.println("🔄 [네이버 콜백] code: " + code + ", state: " + state + ", error: " + error);

        if (error != null) {
            System.err.println("❌ [네이버 콜백] 에러: " + error);
            return redirectToError(error);
        }

        if (code == null || code.trim().isEmpty()) {
            System.err.println("❌ [네이버 콜백] code 없음");
            return redirectToError("no_code");
        }

        try {
            kr.ai.kjun.api.services.oauthservice.naver.dto.NaverUserInfo userInfo = naverService
                    .authenticateAndExtractUser(code, state);

            // 사용자 정보를 User Service에 저장 또는 업데이트 (HTTP 호출)
            Map<String, Object> savedUser = userManagementService.saveOrUpdateUser(
                    "NAVER",
                    userInfo.getExtractedId(),
                    userInfo.getExtractedEmail(),
                    userInfo.getExtractedNickname(),
                    userInfo.getExtractedProfileImage());

            // JWT Access Token 생성 (User Service에서 받은 사용자 ID 사용)
            Long userId = ((Number) savedUser.get("id")).longValue();
            String jwtToken = jwtTokenProvider.generateToken(
                    userId,
                    (String) savedUser.get("email"),
                    (String) savedUser.get("nickname"));

            // Access Token을 Upstash Redis에 저장
            accessTokenService.saveAccessToken(userId, jwtToken);

            // Refresh Token 생성 및 Neon DB에 저장
            String refreshToken = refreshTokenService.generateAndSaveRefreshToken(userId);

            System.out.println("✅ [네이버 콜백] 로그인 성공 - User ID: " + userId);
            return redirectToSuccess(savedUser, jwtToken, refreshToken);

        } catch (Exception e) {
            System.err.println("❌ [네이버 콜백] 로그인 실패: " + e.getMessage());
            e.printStackTrace();
            return redirectToError(e.getMessage());
        }
    }

    // 네이버 로그인 처리 (code 없으면 URL 반환, 있으면 인증 후 JWT 토큰 반환)
    @PostMapping
    public ResponseEntity<Map<String, Object>> naverLogin(@RequestBody(required = false) Map<String, String> request) {
        String code = request != null ? request.get("code") : null;
        String state = request != null ? request.get("state") : null;

        if (code == null || code.trim().isEmpty()) {
            try {
                String loginUrl = naverService.getNaverLoginUrl();
                Map<String, Object> response = new HashMap<>();
                response.put("loginUrl", loginUrl);

                System.out.println("🔗 [네이버 로그인] 로그인 URL 생성: " + loginUrl);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                System.err.println("❌ [네이버 로그인] URL 생성 실패: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildErrorResponse("네이버 로그인 URL 생성 실패: " + e.getMessage()));
            }
        }

        System.out.println("🟢 [네이버 로그인] 진입 - code: " + code);

        try {
            kr.ai.kjun.api.services.oauthservice.naver.dto.NaverUserInfo userInfo = naverService
                    .authenticateAndExtractUser(code, state);

            // 사용자 정보를 User Service에 저장 또는 업데이트 (HTTP 호출)
            Map<String, Object> savedUser = userManagementService.saveOrUpdateUser(
                    "NAVER",
                    userInfo.getExtractedId(),
                    userInfo.getExtractedEmail(),
                    userInfo.getExtractedNickname(),
                    userInfo.getExtractedProfileImage());

            // JWT Access Token 생성 (User Service에서 받은 사용자 ID 사용)
            Long userId = ((Number) savedUser.get("id")).longValue();
            String jwtToken = jwtTokenProvider.generateToken(
                    userId,
                    (String) savedUser.get("email"),
                    (String) savedUser.get("nickname"));

            // Access Token을 Upstash Redis에 저장
            accessTokenService.saveAccessToken(userId, jwtToken);

            // Refresh Token 생성 및 Neon DB에 저장
            String refreshToken = refreshTokenService.generateAndSaveRefreshToken(userId);

            System.out.println("✅ [네이버 로그인] 성공 - User ID: " + userId);
            return ResponseEntity.ok(buildSuccessResponse(savedUser, jwtToken, refreshToken));

        } catch (Exception e) {
            System.err.println("❌ [네이버 로그인] 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("네이버 로그인 실패: " + e.getMessage()));
        }
    }

    // 성공 응답 생성
    private Map<String, Object> buildSuccessResponse(Map<String, Object> user, String jwtToken, String refreshToken) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", jwtToken); // Access Token
        response.put("refreshToken", refreshToken); // Refresh Token

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.get("id"));
        userInfo.put("email", user.get("email"));
        userInfo.put("nickname", user.get("nickname"));
        userInfo.put("profileImage", user.get("profileImageUrl"));
        response.put("user", userInfo);

        return response;
    }

    // 에러 응답 생성
    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        return errorResponse;
    }

    // 성공 리다이렉트
    private ResponseEntity<?> redirectToSuccess(Map<String, Object> user, String jwtToken, String refreshToken) {
        String encodedToken = URLEncoder.encode(jwtToken, StandardCharsets.UTF_8);
        String encodedRefreshToken = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        String encodedEmail = URLEncoder.encode((String) user.get("email"), StandardCharsets.UTF_8);
        String encodedNickname = URLEncoder.encode((String) user.get("nickname"), StandardCharsets.UTF_8);
        Long userId = ((Number) user.get("id")).longValue();

        String redirectUrl = String.format(
                "%s/oauth/naver/success?token=%s&refreshToken=%s&id=%d&email=%s&nickname=%s",
                frontendLoginCallbackUrl, encodedToken, encodedRefreshToken, userId, encodedEmail, encodedNickname);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", redirectUrl)
                .build();
    }

    // 에러 리다이렉트
    private ResponseEntity<?> redirectToError(String error) {
        String encodedError = URLEncoder.encode(error, StandardCharsets.UTF_8);
        String errorUrl = frontendLoginCallbackUrl + "/oauth/naver/error?error=" + encodedError;
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", errorUrl)
                .build();
    }
}
