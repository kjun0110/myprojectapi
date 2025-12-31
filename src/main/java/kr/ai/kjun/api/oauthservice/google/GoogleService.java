package kr.ai.kjun.api.oauthservice.google;

import kr.ai.kjun.api.oauthservice.config.GoogleConfig;
import kr.ai.kjun.api.oauthservice.google.dto.GoogleTokenResponse;
import kr.ai.kjun.api.oauthservice.google.dto.GoogleUserInfo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// 구글 OAuth API 호출 서비스 (RestTemplate 사용, WebFlux 없음)
@Service
public class GoogleService {

    private final RestTemplate restTemplate;
    private final GoogleConfig googleConfig;

    // 구글 API URL
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    public GoogleService(RestTemplate restTemplate, GoogleConfig googleConfig) {
        this.restTemplate = restTemplate;
        this.googleConfig = googleConfig;
    }

    // 구글 로그인 URL 생성
    public String getGoogleLoginUrl() {
        String baseUrl = "https://accounts.google.com/o/oauth2/v2/auth";
        String clientId = googleConfig.getClientId();
        String redirectUri = googleConfig.getRedirectUri();

        // 구글 로그인 시 동의 항목 요청
        // scope: profile (이름, 프로필 이미지), email (이메일)
        String scope = "profile email";

        try {
            // URL 인코딩 적용
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8.toString());

            String googleAuthUrl = String.format(
                    "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&access_type=offline",
                    baseUrl, clientId, encodedRedirectUri, encodedScope);

            System.out.println("🔗 [구글 로그인 URL 생성] " + googleAuthUrl);
            return googleAuthUrl;
        } catch (Exception e) {
            System.err.println("❌ [구글 로그인 URL 생성 실패] " + e.getMessage());
            throw new RuntimeException("구글 로그인 URL 생성 실패", e);
        }
    }

    // Authorization Code로 Access Token 받기
    public GoogleTokenResponse getAccessToken(String code) {
        System.out.println("🔑 [구글 API] Access Token 요청 - code: " + code);

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", googleConfig.getClientId());
        params.add("client_secret", googleConfig.getClientSecret());
        params.add("redirect_uri", googleConfig.getRedirectUri());
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
                    GOOGLE_TOKEN_URL,
                    request,
                    GoogleTokenResponse.class);

            GoogleTokenResponse tokenResponse = response.getBody();
            if (tokenResponse != null) {
                System.out.println("✅ [구글 API] Access Token 받기 성공");
                return tokenResponse;
            } else {
                throw new RuntimeException("구글 토큰 응답이 null입니다");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err
                    .println("❌ [구글 API] Access Token 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("구글 Access Token 발급 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            System.err.println("❌ [구글 API] Access Token 실패: " + e.getMessage());
            throw new RuntimeException("구글 Access Token 발급 실패", e);
        }
    }

    // Access Token으로 사용자 정보 받기
    public GoogleUserInfo getUserInfo(String accessToken) {
        System.out.println("👤 [구글 API] 사용자 정보 요청");

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    GoogleUserInfo.class);

            GoogleUserInfo userInfo = response.getBody();
            if (userInfo != null) {
                System.out.println("✅ [구글 API] 사용자 정보 받기 성공 - ID: " + userInfo.getId());
                return userInfo;
            } else {
                throw new RuntimeException("구글 사용자 정보 응답이 null입니다");
            }
        } catch (Exception e) {
            System.err.println("❌ [구글 API] 사용자 정보 실패: " + e.getMessage());
            throw new RuntimeException("구글 사용자 정보 조회 실패", e);
        }
    }

    // 구글 인증 및 사용자 정보 추출
    public GoogleUserInfo authenticateAndExtractUser(String code) {
        GoogleTokenResponse tokenResponse = getAccessToken(code);
        GoogleUserInfo userInfo = getUserInfo(tokenResponse.getAccessToken());
        System.out.println("✅ [구글 인증] 완료 - ID: " + userInfo.getId());
        return userInfo;
    }
}
