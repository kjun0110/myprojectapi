package kr.ai.kjun.api.services.oauthservice.kakao;

import kr.ai.kjun.api.services.oauthservice.config.KakaoConfig;
import kr.ai.kjun.api.services.oauthservice.kakao.dto.KakaoTokenResponse;
import kr.ai.kjun.api.services.oauthservice.kakao.dto.KakaoUserInfo;
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

// 카카오 OAuth API 호출 서비스 (RestTemplate 사용, WebFlux 없음)
@Service
public class KakaoService {

    private final RestTemplate restTemplate;
    private final KakaoConfig kakaoConfig;

    // 카카오 API URL
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    public KakaoService(RestTemplate restTemplate, KakaoConfig kakaoConfig) {
        this.restTemplate = restTemplate;
        this.kakaoConfig = kakaoConfig;
    }

    // 카카오 로그인 URL 생성
    public String getKakaoLoginUrl() {
        String baseUrl = "https://kauth.kakao.com/oauth/authorize";
        String clientId = kakaoConfig.getRestApiKey();
        String redirectUri = kakaoConfig.getRedirectUri();

        // 카카오 로그인 시 동의 항목 요청 (scope 파라미터 추가)
        // profile_nickname: 닉네임, profile_image: 프로필 이미지
        // account_email은 카카오 개발자 콘솔에서 설정되지 않아서 제외
        String scope = "profile_nickname,profile_image";

        try {
            // URL 인코딩 적용 (redirect_uri와 scope 모두 인코딩)
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8.toString());

            String kakaoAuthUrl = String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s",
                    baseUrl, clientId, encodedRedirectUri, encodedScope);

            System.out.println("🔗 [카카오 로그인 URL 생성] " + kakaoAuthUrl);
            return kakaoAuthUrl;
        } catch (Exception e) {
            System.err.println("❌ [카카오 로그인 URL 생성 실패] " + e.getMessage());
            throw new RuntimeException("카카오 로그인 URL 생성 실패", e);
        }
    }

    // Authorization Code로 Access Token 받기
    public KakaoTokenResponse getAccessToken(String code) {
        System.out.println("🔑 [카카오 API] Access Token 요청 - code: " + code);

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoConfig.getRestApiKey());
        params.add("redirect_uri", kakaoConfig.getRedirectUri());
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                    KAKAO_TOKEN_URL,
                    request,
                    KakaoTokenResponse.class);

            KakaoTokenResponse tokenResponse = response.getBody();
            if (tokenResponse != null) {
                System.out.println("✅ [카카오 API] Access Token 받기 성공");
                return tokenResponse;
            } else {
                throw new RuntimeException("카카오 토큰 응답이 null입니다");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err
                    .println("❌ [카카오 API] Access Token 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("카카오 Access Token 발급 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            System.err.println("❌ [카카오 API] Access Token 실패: " + e.getMessage());
            throw new RuntimeException("카카오 Access Token 발급 실패", e);
        }
    }

    // Access Token으로 사용자 정보 받기
    public KakaoUserInfo getUserInfo(String accessToken) {
        System.out.println("👤 [카카오 API] 사용자 정보 요청");

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    KakaoUserInfo.class);

            KakaoUserInfo userInfo = response.getBody();
            if (userInfo != null) {
                System.out.println("✅ [카카오 API] 사용자 정보 받기 성공 - ID: " + userInfo.getId());
                return userInfo;
            } else {
                throw new RuntimeException("카카오 사용자 정보 응답이 null입니다");
            }
        } catch (Exception e) {
            System.err.println("❌ [카카오 API] 사용자 정보 실패: " + e.getMessage());
            throw new RuntimeException("카카오 사용자 정보 조회 실패", e);
        }
    }

    // 카카오 인증 및 사용자 정보 추출
    public KakaoUserInfo authenticateAndExtractUser(String code) {
        KakaoTokenResponse tokenResponse = getAccessToken(code);
        KakaoUserInfo userInfo = getUserInfo(tokenResponse.getAccessToken());
        System.out.println("✅ [카카오 인증] 완료 - ID: " + userInfo.getId());
        return userInfo;
    }
}
