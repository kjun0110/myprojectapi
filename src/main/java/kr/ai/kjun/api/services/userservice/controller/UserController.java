package kr.ai.kjun.api.services.userservice.controller;

import kr.ai.kjun.api.services.userservice.entity.User;
import kr.ai.kjun.api.services.userservice.model.UserModel;
import kr.ai.kjun.api.services.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 REST API 컨트롤러
 * OAuth 서비스에서 HTTP로 호출
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * OAuth 제공자와 OAuth ID로 사용자 조회
     * GET /api/users/oauth?provider=KAKAO&oauthId=123456
     */
    @GetMapping("/oauth")
    public ResponseEntity<Map<String, Object>> findByOAuth(
            @RequestParam String provider,
            @RequestParam String oauthId) {

        try {
            User.OAuthProvider oauthProvider = User.OAuthProvider.valueOf(provider.toUpperCase());
            Optional<User> userOpt = userService.findByOAuthProviderAndOAuthId(oauthProvider, oauthId);

            if (userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("user", toModel(userOpt.get()));
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "사용자를 찾을 수 없습니다");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "유효하지 않은 OAuth 제공자입니다");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * OAuth 로그인 사용자 저장 또는 업데이트
     * POST /api/users/oauth
     */
    @PostMapping("/oauth")
    public ResponseEntity<Map<String, Object>> saveOrUpdateUser(@RequestBody Map<String, Object> request) {
        try {
            String provider = (String) request.get("provider");
            String oauthId = (String) request.get("oauthId");
            String email = (String) request.get("email");
            String nickname = (String) request.get("nickname");
            String profileImageUrl = (String) request.get("profileImageUrl");

            if (provider == null || oauthId == null || email == null || nickname == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "필수 파라미터가 누락되었습니다");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            User.OAuthProvider oauthProvider = User.OAuthProvider.valueOf(provider.toUpperCase());
            User savedUser = userService.saveOrUpdateUser(oauthProvider, oauthId, email, nickname, profileImageUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", toModel(savedUser));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "유효하지 않은 OAuth 제공자입니다");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "사용자 저장 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 사용자 ID로 사용자 조회
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable Long userId) {
        Optional<User> userOpt = userService.findById(userId);

        if (userOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", toModel(userOpt.get()));
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "사용자를 찾을 수 없습니다");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * User 엔티티를 UserModel로 변환
     * 
     * @param user User 엔티티
     * @return UserModel
     */
    private UserModel toModel(User user) {
        return UserModel.builder()
                .id(user.getId())
                .email(user.getEmail())
                .oauthProvider(user.getOauthProvider().name())
                .oauthId(user.getOauthId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
