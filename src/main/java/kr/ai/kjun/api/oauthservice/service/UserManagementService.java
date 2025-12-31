package kr.ai.kjun.api.oauthservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 관리 서비스
 * OAuth 서비스에서 User Service를 HTTP로 호출하여 사용자 정보 관리
 */
@Service
public class UserManagementService {

    private final RestTemplate restTemplate;

    @Value("${USER_SERVICE_URL:http://localhost:8080}")
    private String userServiceUrl;

    public UserManagementService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * OAuth 로그인 사용자 저장 또는 업데이트
     * 
     * @param oauthProvider   OAuth 제공자 (KAKAO, NAVER, GOOGLE)
     * @param oauthId         OAuth ID
     * @param email           이메일
     * @param nickname        닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @return 저장된 사용자 정보 (id, email, nickname 등)
     */
    public Map<String, Object> saveOrUpdateUser(
            String oauthProvider,
            String oauthId,
            String email,
            String nickname,
            String profileImageUrl) {

        String url = userServiceUrl + "/api/users/oauth";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("provider", oauthProvider);
        requestBody.put("oauthId", oauthId);
        requestBody.put("email", email);
        requestBody.put("nickname", nickname);
        if (profileImageUrl != null) {
            requestBody.put("profileImageUrl", profileImageUrl);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> user = (Map<String, Object>) responseBody.get("user");
                    return user;
                } else {
                    throw new RuntimeException("User Service 응답 실패: " + responseBody.get("message"));
                }
            } else {
                throw new RuntimeException("User Service 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ [User Service 호출 실패] " + e.getMessage());
            throw new RuntimeException("User Service 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자 ID로 사용자 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보 (id, email, nickname 등)
     */
    public Map<String, Object> findById(Long userId) {
        String url = userServiceUrl + "/api/users/" + userId;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> user = (Map<String, Object>) responseBody.get("user");
                    return user;
                } else {
                    throw new RuntimeException("User Service 응답 실패: " + responseBody.get("message"));
                }
            } else {
                throw new RuntimeException("User Service 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ [User Service 호출 실패] " + e.getMessage());
            throw new RuntimeException("User Service 호출 실패: " + e.getMessage(), e);
        }
    }
}
