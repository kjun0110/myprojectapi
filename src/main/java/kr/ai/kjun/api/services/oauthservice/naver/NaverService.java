package kr.ai.kjun.api.services.oauthservice.naver;

import kr.ai.kjun.api.services.oauthservice.config.NaverConfig;
import kr.ai.kjun.api.services.oauthservice.naver.dto.NaverTokenResponse;
import kr.ai.kjun.api.services.oauthservice.naver.dto.NaverUserInfo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// 네이버 OAuth API 호출 서비스 (RestTemplate 사용, WebFlux 없음)
@Service
public class NaverService {

    private final RestTemplate restTemplate;
    private final NaverConfig naverConfig;

    // 네이버 API URL
    private static final String NAVER_TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    public NaverService(RestTemplate restTemplate, NaverConfig naverConfig) {
        this.restTemplate = restTemplate;
        this.naverConfig = naverConfig;
    }

    // 네이버 로그인 URL 생성
    public String getNaverLoginUrl() {
        String baseUrl = "https://nid.naver.com/oauth2.0/authorize";
        String clientId = naverConfig.getClientId();
        String redirectUri = naverConfig.getRedirectUri();

        // 네이버 로그인 시 동의 항목 요청 (state는 CSRF 방지용)
        String state = generateRandomState();

        try {
            // URL 인코딩 적용
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8.toString());

            String naverAuthUrl = String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&state=%s",
                    baseUrl, clientId, encodedRedirectUri, encodedState);

            System.out.println("🔗 [네이버 로그인 URL 생성] " + naverAuthUrl);
            return naverAuthUrl;
        } catch (Exception e) {
            System.err.println("❌ [네이버 로그인 URL 생성 실패] " + e.getMessage());
            throw new RuntimeException("네이버 로그인 URL 생성 실패", e);
        }
    }

    // CSRF 방지용 랜덤 state 생성
    private String generateRandomState() {
        return String.valueOf(System.currentTimeMillis());
    }

    // Authorization Code로 Access Token 받기
    public NaverTokenResponse getAccessToken(String code, String state) {
        System.out.println("🔑 [네이버 API] Access Token 요청 - code: " + code);

        try {
            // URL 파라미터로 전송
            String url = String.format(
                    "%s?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&state=%s",
                    NAVER_TOKEN_URL,
                    naverConfig.getClientId(),
                    naverConfig.getClientSecret(),
                    code,
                    state != null ? state : "");

            ResponseEntity<NaverTokenResponse> response = restTemplate.getForEntity(
                    url,
                    NaverTokenResponse.class);

            NaverTokenResponse tokenResponse = response.getBody();
            if (tokenResponse != null) {
                if (tokenResponse.getError() != null) {
                    System.err.println("❌ [네이버 API] Access Token 실패: " + tokenResponse.getError() + " - "
                            + tokenResponse.getErrorDescription());
                    throw new RuntimeException(
                            "네이버 Access Token 발급 실패: " + tokenResponse.getError() + " - "
                                    + tokenResponse.getErrorDescription());
                }
                System.out.println("✅ [네이버 API] Access Token 받기 성공");
                return tokenResponse;
            } else {
                throw new RuntimeException("네이버 토큰 응답이 null입니다");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err
                    .println("❌ [네이버 API] Access Token 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("네이버 Access Token 발급 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            System.err.println("❌ [네이버 API] Access Token 실패: " + e.getMessage());
            throw new RuntimeException("네이버 Access Token 발급 실패", e);
        }
    }

    // Access Token으로 사용자 정보 받기
    public NaverUserInfo getUserInfo(String accessToken) {
        System.out.println("👤 [네이버 API] 사용자 정보 요청");

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<NaverUserInfo> response = restTemplate.exchange(
                    NAVER_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    NaverUserInfo.class);

            NaverUserInfo userInfo = response.getBody();
            if (userInfo != null) {
                if (!"00".equals(userInfo.getResultCode())) {
                    System.err.println("❌ [네이버 API] 사용자 정보 실패: " + userInfo.getMessage());
                    throw new RuntimeException("네이버 사용자 정보 조회 실패: " + userInfo.getMessage());
                }
                System.out.println("✅ [네이버 API] 사용자 정보 받기 성공 - ID: " + userInfo.getExtractedId());
                return userInfo;
            } else {
                throw new RuntimeException("네이버 사용자 정보 응답이 null입니다");
            }
        } catch (Exception e) {
            System.err.println("❌ [네이버 API] 사용자 정보 실패: " + e.getMessage());
            throw new RuntimeException("네이버 사용자 정보 조회 실패", e);
        }
    }

    // 네이버 인증 및 사용자 정보 추출
    public NaverUserInfo authenticateAndExtractUser(String code, String state) {
        NaverTokenResponse tokenResponse = getAccessToken(code, state);
        NaverUserInfo userInfo = getUserInfo(tokenResponse.getAccessToken());
        System.out.println("✅ [네이버 인증] 완료 - ID: " + userInfo.getExtractedId());
        return userInfo;
    }
}
