# OAuth 2.0 구현 완전 가이드

## 📚 목차

1. [OAuth 2.0 기본 개념](#1-oauth-20-기본-개념)
2. [전체 아키텍처](#2-전체-아키텍처)
3. [카카오 OAuth 구현](#3-카카오-oauth-구현)
4. [네이버 OAuth 구현](#4-네이버-oauth-구현)
5. [JWT 토큰 생성](#5-jwt-토큰-생성)
6. [데이터 흐름 상세 분석](#6-데이터-흐름-상세-분석)
7. [주요 차이점 비교](#7-주요-차이점-비교)
8. [실전 팁과 주의사항](#8-실전-팁과-주의사항)

---

## 1. OAuth 2.0 기본 개념

### 1.1 OAuth 2.0이란?

OAuth 2.0은 **인증(Authentication)과 인가(Authorization)를 위한 표준 프로토콜**입니다.

- **인증(Authentication)**: 사용자가 누구인지 확인
- **인가(Authorization)**: 사용자가 특정 리소스에 접근할 권한이 있는지 확인

### 1.2 OAuth 2.0의 핵심 역할

```
사용자 (Resource Owner)
    ↓
프론트엔드 (Client)
    ↓
백엔드 (Authorization Server)
    ↓
카카오/네이버 (Resource Server)
```

### 1.3 OAuth 2.0 인증 흐름 (Authorization Code Flow)

```
1. 사용자가 "카카오로 로그인" 버튼 클릭
   ↓
2. 프론트엔드 → 백엔드: 로그인 URL 요청
   ↓
3. 백엔드 → 프론트엔드: 카카오 로그인 URL 반환
   ↓
4. 프론트엔드: 사용자를 카카오 로그인 페이지로 리다이렉트
   ↓
5. 사용자가 카카오에서 로그인 및 동의
   ↓
6. 카카오 → 백엔드: Authorization Code를 콜백 URL로 전송
   ↓
7. 백엔드 → 카카오: Code를 Access Token으로 교환
   ↓
8. 백엔드 → 카카오: Access Token으로 사용자 정보 조회
   ↓
9. 백엔드: JWT 토큰 생성 후 프론트엔드로 전달
```

---

## 2. 전체 아키텍처

### 2.1 시스템 구조

```
┌─────────────────────────────────────────────────────────────┐
│                    프론트엔드 (Frontend)                       │
│              React/Next.js - localhost:3000                   │
│                                                               │
│  주요 파일:                                                   │
│  - page.tsx (로그인 페이지)                                   │
│  - /auth/kakao/success (성공 페이지)                          │
│  - /auth/naver/success (성공 페이지)                          │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ HTTP 요청 (JSON)
                        │ POST /api/auth/kakao/login
                        │ POST /api/auth/kakao
                        │ POST /api/auth/naver/login
                        │ POST /api/auth/naver
                        ↓
┌─────────────────────────────────────────────────────────────┐
│              Gateway (Spring Cloud Gateway)                  │
│                    localhost:8080                            │
│                                                               │
│  설정 파일:                                                   │
│  - gateway/src/main/resources/application.yaml               │
│                                                               │
│  주요 기능:                                                   │
│  ├─ CORS 설정 (globalcors)                                   │
│  │  └─ allowedOrigins: http://localhost:3000                │
│  │                                                             │
│  ├─ 라우팅 설정 (routes)                                     │
│  │  ├─ auth-service: /api/auth/** → authservice:8081         │
│  │  ├─ kakao-callback: /auth/kakao/callback → /kakao/callback│
│  │  └─ naver-callback: /auth/naver/callback → /naver/callback│
│  │                                                             │
│  └─ 경로 변환 (RewritePath)                                   │
│     └─ /api/auth/(?<segment>.*) → /${segment}                │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ 라우팅 및 CORS 처리 후
                        │ HTTP 요청 전달
                        ↓
┌─────────────────────────────────────────────────────────────┐
│              Auth Service (Spring Boot)                      │
│                  authservice:8081                           │
│                                                               │
│  설정 파일:                                                   │
│  - services/authservice/src/main/resources/application.yaml  │
│    ├─ kakao.rest-api-key: ${KAKAO_REST_API_KEY}            │
│    ├─ kakao.redirect-uri: ${KAKAO_REDIRECT_URI}            │
│    ├─ naver.client-id: ${NAVER_CLIENT_ID}                   │
│    ├─ naver.client-secret: ${NAVER_CLIENT_SECRET}           │
│    ├─ naver.redirect-uri: ${NAVER_REDIRECT_URI}              │
│    └─ jwt.secret: ${JWT_SECRET}                              │
│                                                               │
│  메인 애플리케이션:                                            │
│  - ApiApplication.java                                       │
│                                                               │
│  카카오 OAuth 패키지:                                         │
│  services/authservice/src/main/java/kr/ai/kjun/api/kakao/   │
│  ├─ KakaoController.java                                    │
│  │  ├─ POST /kakao/login → getKakaoLoginUrl()              │
│  │  ├─ GET /kakao/callback → kakaoCallback()               │
│  │  └─ POST /kakao → kakaoLogin()                           │
│  │                                                           │
│  ├─ KakaoService.java                                        │
│  │  ├─ getKakaoLoginUrl() → 카카오 로그인 URL 생성           │
│  │  ├─ getAccessToken(code) → Access Token 받기             │
│  │  ├─ getUserInfo(accessToken) → 사용자 정보 받기         │
│  │  └─ authenticateAndExtractUser(code) → 전체 인증 흐름    │
│  │                                                           │
│  └─ dto/                                                     │
│     ├─ KakaoTokenResponse.java                              │
│     └─ KakaoUserInfo.java                                   │
│                                                               │
│  네이버 OAuth 패키지:                                         │
│  services/authservice/src/main/java/kr/ai/kjun/api/naver/   │
│  ├─ NaverController.java                                    │
│  │  ├─ POST /naver/login → getNaverLoginUrl()              │
│  │  ├─ GET /naver/callback → naverCallback()              │
│  │  └─ POST /naver → naverLogin()                          │
│  │                                                           │
│  ├─ NaverService.java                                        │
│  │  ├─ getNaverLoginUrl() → 네이버 로그인 URL 생성          │
│  │  ├─ getAccessToken(code, state) → Access Token 받기       │
│  │  ├─ getUserInfo(accessToken) → 사용자 정보 받기         │
│  │  └─ authenticateAndExtractUser(code, state) → 전체 인증  │
│  │                                                           │
│  └─ dto/                                                     │
│     ├─ NaverTokenResponse.java                              │
│     └─ NaverUserInfo.java                                   │
│                                                               │
│  JWT 패키지:                                                  │
│  services/authservice/src/main/java/kr/ai/kjun/api/jwt/     │
│  ├─ JwtTokenProvider.java                                    │
│  │  ├─ generateToken(userId, email, nickname) → JWT 생성    │
│  │  ├─ validateToken(token) → 토큰 검증                     │
│  │  └─ getUserIdFromToken(token) → 사용자 ID 추출           │
│  │                                                           │
│  └─ JwtProperties.java                                       │
│     └─ JWT 설정 (secret, expiration)                        │
│                                                               │
│  Config 패키지:                                               │
│  services/authservice/src/main/java/kr/ai/kjun/api/config/   │
│  ├─ KakaoConfig.java                                         │
│  │  └─ @ConfigurationProperties(prefix = "kakao")          │
│  │                                                           │
│  ├─ NaverConfig.java                                         │
│  │  └─ @ConfigurationProperties(prefix = "naver")           │
│  │                                                           │
│  └─ RestTemplateConfig.java                                  │
│     └─ RestTemplate 빈 생성                                  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ OAuth API 호출
                        │ (RestTemplate 사용)
                        ↓
        ┌───────────────────────────────┐
        │                               │
        ↓                               ↓
┌──────────────────┐          ┌──────────────────┐
│   카카오 OAuth API │          │  네이버 OAuth API  │
│                   │          │                   │
│ API 엔드포인트:    │          │ API 엔드포인트:    │
│ ├─ Authorization:│          │ ├─ Authorization:│
│ │  kauth.kakao.  │          │ │  nid.naver.com │
│ │  com/oauth/    │          │ │  /oauth2.0/    │
│ │  authorize     │          │ │  authorize     │
│ │                 │          │ │                 │
│ ├─ Token:        │          │ ├─ Token:        │
│ │  kauth.kakao.  │          │ │  nid.naver.com │
│ │  com/oauth/    │          │ │  /oauth2.0/    │
│ │  token         │          │ │  token         │
│ │                 │          │ │                 │
│ └─ User Info:    │          │ └─ User Info:    │
│    kapi.kakao.   │          │    openapi.naver.│
│    com/v2/user/me│          │    com/v1/nid/me │
└──────────────────┘          └──────────────────┘
```

### 2.1.1 파일 구조 상세

#### Gateway 파일 구조
```
gateway/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── kr/ai/kjun/api/
│   │   │       └── ApiApplication.java
│   │   └── resources/
│   │       └── application.yaml          ← Gateway 설정 파일
│   │           ├─ CORS 설정
│   │           ├─ 라우팅 설정
│   │           └─ 경로 변환 설정
│   └── test/
└── Dockerfile
```

#### Auth Service 파일 구조
```
services/authservice/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── kr/ai/kjun/api/
│   │   │       ├── ApiApplication.java
│   │   │       │
│   │   │       ├── config/               ← 설정 클래스
│   │   │       │   ├── KakaoConfig.java
│   │   │       │   ├── NaverConfig.java
│   │   │       │   └── RestTemplateConfig.java
│   │   │       │
│   │   │       ├── kakao/                 ← 카카오 OAuth
│   │   │       │   ├── KakaoController.java
│   │   │       │   ├── KakaoService.java
│   │   │       │   └── dto/
│   │   │       │       ├── KakaoTokenResponse.java
│   │   │       │       └── KakaoUserInfo.java
│   │   │       │
│   │   │       ├── naver/                 ← 네이버 OAuth
│   │   │       │   ├── NaverController.java
│   │   │       │   ├── NaverService.java
│   │   │       │   └── dto/
│   │   │       │       ├── NaverTokenResponse.java
│   │   │       │       └── NaverUserInfo.java
│   │   │       │
│   │   │       └── jwt/                   ← JWT 토큰
│   │   │           ├── JwtTokenProvider.java
│   │   │           └── JwtProperties.java
│   │   │
│   │   └── resources/
│   │       └── application.yaml           ← Auth Service 설정 파일
│   │           ├─ kakao 설정
│   │           ├─ naver 설정
│   │           └─ jwt 설정
│   └── test/
└── Dockerfile
```

### 2.2 주요 컴포넌트

#### 2.2.1 Gateway (Spring Cloud Gateway)
- **역할**: 라우팅, CORS 처리, 경로 변환
- **포트**: 8080
- **설정 파일**: `gateway/src/main/resources/application.yaml`

#### 2.2.2 Auth Service (Spring Boot)
- **역할**: OAuth 인증 처리, JWT 토큰 생성
- **포트**: 8081
- **주요 클래스**:
  - `KakaoController`, `KakaoService`
  - `NaverController`, `NaverService`
  - `JwtTokenProvider`

---

## 3. 카카오 OAuth 구현

### 3.1 카카오 OAuth 설정

#### 3.1.1 환경 변수 설정

`.env` 파일:
```env
KAKAO_REST_API_KEY=your_kakao_rest_api_key
KAKAO_REDIRECT_URI=http://localhost:8080/auth/kakao/callback
FRONT_LOGIN_CALLBACK_URL=http://localhost:3000
```

#### 3.1.2 Config 클래스

```java
@ConfigurationProperties(prefix = "kakao")
@Component
public class KakaoConfig {
    private String restApiKey;
    private String redirectUri;
    // getter, setter
}
```

`application.yaml`:
```yaml
kakao:
  rest-api-key: ${KAKAO_REST_API_KEY}
  redirect-uri: ${KAKAO_REDIRECT_URI}
```

### 3.2 카카오 OAuth 흐름 상세

#### Step 1: 로그인 URL 생성

**엔드포인트**: `POST /api/auth/kakao/login`

**프론트엔드 요청**:
```javascript
fetch('http://localhost:8080/api/auth/kakao/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  }
})
```

**백엔드 처리** (`KakaoService.getKakaoLoginUrl()`):
```java
public String getKakaoLoginUrl() {
    String baseUrl = "https://kauth.kakao.com/oauth/authorize";
    String clientId = kakaoConfig.getRestApiKey();
    String redirectUri = kakaoConfig.getRedirectUri();
    String scope = "profile_nickname,profile_image";
    
    // URL 인코딩
    String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8);
    
    // 최종 URL 생성
    String kakaoAuthUrl = String.format(
        "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s",
        baseUrl, clientId, encodedRedirectUri, encodedScope
    );
    
    return kakaoAuthUrl;
}
```

**생성되는 URL 예시**:
```
https://kauth.kakao.com/oauth/authorize?
  client_id=YOUR_CLIENT_ID&
  redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fauth%2Fkakao%2Fcallback&
  response_type=code&
  scope=profile_nickname%2Cprofile_image
```

**백엔드 응답**:
```json
{
  "success": true,
  "loginUrl": "https://kauth.kakao.com/oauth/authorize?..."
}
```

#### Step 2: 사용자 인증 및 Authorization Code 받기

**프론트엔드**: 생성된 URL로 사용자를 리다이렉트
```javascript
window.location.href = response.loginUrl;
```

**카카오**: 사용자가 로그인 및 동의 후 콜백 URL로 리다이렉트
```
http://localhost:8080/auth/kakao/callback?code=AUTHORIZATION_CODE
```

#### Step 3: Authorization Code로 Access Token 받기

**엔드포인트**: `GET /auth/kakao/callback?code=...`

**백엔드 처리** (`KakaoService.getAccessToken()`):
```java
public KakaoTokenResponse getAccessToken(String code) {
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
    
    // 카카오 API 호출
    ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
        "https://kauth.kakao.com/oauth/token",
        request,
        KakaoTokenResponse.class
    );
    
    return response.getBody();
}
```

**카카오 API 요청 형식**:
- **Content-Type**: `application/x-www-form-urlencoded`
- **요청 Body**: `grant_type=authorization_code&client_id=...&redirect_uri=...&code=...`

**카카오 API 응답** (JSON):
```json
{
  "access_token": "ACCESS_TOKEN",
  "token_type": "bearer",
  "refresh_token": "REFRESH_TOKEN",
  "expires_in": 21599,
  "scope": "profile_nickname profile_image"
}
```

#### Step 4: Access Token으로 사용자 정보 받기

**백엔드 처리** (`KakaoService.getUserInfo()`):
```java
public KakaoUserInfo getUserInfo(String accessToken) {
    // 요청 헤더 설정
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    
    HttpEntity<String> request = new HttpEntity<>(headers);
    
    // 카카오 API 호출
    ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
        "https://kapi.kakao.com/v2/user/me",
        HttpMethod.GET,
        request,
        KakaoUserInfo.class
    );
    
    return response.getBody();
}
```

**카카오 API 요청**:
- **Method**: GET
- **Header**: `Authorization: Bearer ACCESS_TOKEN`
- **URL**: `https://kapi.kakao.com/v2/user/me`

**카카오 API 응답** (JSON):
```json
{
  "id": 123456789,
  "kakao_account": {
    "email": "user@example.com",
    "profile": {
      "nickname": "사용자",
      "profile_image_url": "https://..."
    }
  }
}
```

#### Step 5: JWT 토큰 생성 및 프론트엔드로 전달

**백엔드 처리** (`KakaoController.handleKakaoCallback()`):
```java
// 1. 사용자 정보 추출
KakaoUserInfo userInfo = kakaoService.authenticateAndExtractUser(code);

// 2. JWT 토큰 생성
String jwtToken = jwtTokenProvider.generateToken(
    userInfo.getId(),
    userInfo.getExtractedEmail(),
    userInfo.getExtractedNickname()
);

// 3. 프론트엔드로 리다이렉트
String redirectUrl = String.format(
    "%s/auth/kakao/success?token=%s&id=%d&email=%s&nickname=%s",
    frontendLoginCallbackUrl, encodedToken, userInfo.getId(), 
    encodedEmail, encodedNickname
);

return ResponseEntity.status(HttpStatus.FOUND)
    .header("Location", redirectUrl)
    .build();
```

**프론트엔드 리다이렉트 URL**:
```
http://localhost:3000/auth/kakao/success?
  token=JWT_TOKEN&
  id=123456789&
  email=user@example.com&
  nickname=사용자
```

### 3.3 카카오 DTO 구조

#### KakaoTokenResponse
```java
public class KakaoTokenResponse {
    private String accessToken;
    private String tokenType;
    private String refreshToken;
    private Integer expiresIn;
    private String scope;
    // getter, setter
}
```

#### KakaoUserInfo
```java
public class KakaoUserInfo {
    private Long id;
    private KakaoAccount kakaoAccount;
    
    // 편의 메서드
    public String getExtractedEmail() {
        return kakaoAccount != null ? kakaoAccount.getEmail() : null;
    }
    
    public String getExtractedNickname() {
        return kakaoAccount != null && kakaoAccount.getProfile() != null
            ? kakaoAccount.getProfile().getNickname() : null;
    }
}
```

---

## 4. 네이버 OAuth 구현

### 4.1 네이버 OAuth 설정

#### 4.1.1 환경 변수 설정

`.env` 파일:
```env
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret
NAVER_REDIRECT_URI=http://localhost:8080/auth/naver/callback
FRONT_LOGIN_CALLBACK_URL=http://localhost:3000
```

#### 4.1.2 Config 클래스

```java
@ConfigurationProperties(prefix = "naver")
@Component
public class NaverConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    // getter, setter
}
```

### 4.2 네이버 OAuth 흐름 상세

#### Step 1: 로그인 URL 생성

**엔드포인트**: `POST /api/auth/naver/login`

**백엔드 처리** (`NaverService.getNaverLoginUrl()`):
```java
public String getNaverLoginUrl() {
    String baseUrl = "https://nid.naver.com/oauth2.0/authorize";
    String clientId = naverConfig.getClientId();
    String redirectUri = naverConfig.getRedirectUri();
    
    // CSRF 방지용 state 생성
    String state = generateRandomState();
    
    // URL 인코딩
    String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
    
    // 최종 URL 생성
    String naverAuthUrl = String.format(
        "%s?client_id=%s&redirect_uri=%s&response_type=code&state=%s",
        baseUrl, clientId, encodedRedirectUri, encodedState
    );
    
    return naverAuthUrl;
}
```

**생성되는 URL 예시**:
```
https://nid.naver.com/oauth2.0/authorize?
  client_id=YOUR_CLIENT_ID&
  redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fauth%2Fnaver%2Fcallback&
  response_type=code&
  state=1234567890123
```

#### Step 2: Authorization Code로 Access Token 받기

**백엔드 처리** (`NaverService.getAccessToken()`):
```java
public NaverTokenResponse getAccessToken(String code, String state) {
    // URL 파라미터로 전송 (카카오와 다름!)
    String url = String.format(
        "%s?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&state=%s",
        "https://nid.naver.com/oauth2.0/token",
        naverConfig.getClientId(),
        naverConfig.getClientSecret(),
        code,
        state != null ? state : ""
    );
    
    // GET 요청으로 전송
    ResponseEntity<NaverTokenResponse> response = restTemplate.getForEntity(
        url,
        NaverTokenResponse.class
    );
    
    return response.getBody();
}
```

**네이버 API 요청 형식**:
- **Method**: GET
- **URL 파라미터**: `grant_type=authorization_code&client_id=...&client_secret=...&code=...&state=...`

**네이버 API 응답** (JSON):
```json
{
  "access_token": "ACCESS_TOKEN",
  "refresh_token": "REFRESH_TOKEN",
  "token_type": "bearer",
  "expires_in": 3600,
  "error": null,
  "error_description": null
}
```

#### Step 3: Access Token으로 사용자 정보 받기

**백엔드 처리** (`NaverService.getUserInfo()`):
```java
public NaverUserInfo getUserInfo(String accessToken) {
    // 요청 헤더 설정
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    
    HttpEntity<String> request = new HttpEntity<>(headers);
    
    // 네이버 API 호출
    ResponseEntity<NaverUserInfo> response = restTemplate.exchange(
        "https://openapi.naver.com/v1/nid/me",
        HttpMethod.GET,
        request,
        NaverUserInfo.class
    );
    
    return response.getBody();
}
```

**네이버 API 응답** (JSON):
```json
{
  "resultcode": "00",
  "message": "success",
  "response": {
    "id": "네이버ID",
    "email": "user@example.com",
    "nickname": "사용자",
    "profile_image": "https://...",
    "name": "홍길동",
    "gender": "M",
    "age": "20-29",
    "birthday": "01-01",
    "birthyear": "1990",
    "mobile": "010-1234-5678"
  }
}
```

### 4.3 네이버와 카카오의 주요 차이점

| 항목 | 카카오 | 네이버 |
|------|--------|--------|
| **Access Token 요청 방식** | POST (Form URL Encoded) | GET (URL 파라미터) |
| **Client Secret** | 필요 없음 | 필요함 |
| **State 파라미터** | 선택적 | 필수 (CSRF 방지) |
| **사용자 ID 타입** | Long (숫자) | String (문자열) |
| **응답 구조** | `kakao_account` 객체 | `response` 객체 |
| **에러 처리** | HTTP 상태 코드 | `resultcode` 필드 |

---

## 5. JWT 토큰 생성

### 5.1 JWT란?

**JWT (JSON Web Token)**는 인증 정보를 안전하게 전달하기 위한 토큰 형식입니다.

**구조**:
```
header.payload.signature
```

### 5.2 JWT 토큰 생성 과정

**코드** (`JwtTokenProvider.generateToken()`):
```java
public String generateToken(Long userId, String email, String nickname) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());
    
    // Claims (페이로드) 설정
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("email", email);
    claims.put("nickname", nickname);
    
    // JWT 토큰 생성
    return Jwts.builder()
        .claims(claims)
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(secretKey)  // HMAC-SHA 알고리즘으로 서명
        .compact();
}
```

**생성되는 JWT 토큰 예시**:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJ1c2VySWQiOjEyMzQ1Njc4OSwiZW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIiwibmlja25hbWUiOiLsgYzsiqTtirgiLCJzdWIiOiIxMjM0NTY3ODkiLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MTcwMDA4NjQwMH0.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### 5.3 JWT 토큰 검증

**코드** (`JwtTokenProvider.validateToken()`):
```java
public boolean validateToken(String token) {
    try {
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token);
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

---

## 6. 데이터 흐름 상세 분석

### 6.1 전체 데이터 흐름도

```
[프론트엔드]                    [백엔드]                    [카카오/네이버]
     │                            │                              │
     │ 1. POST /api/auth/kakao/login                            │
     │───────────────────────────>│                              │
     │                            │                              │
     │ 2. { "loginUrl": "..." }  │                              │
     │<───────────────────────────│                              │
     │                            │                              │
     │ 3. window.location.href = loginUrl                       │
     │──────────────────────────────────────────────────────────>│
     │                            │                              │
     │                            │ 4. 사용자 로그인 및 동의      │
     │                            │                              │
     │                            │ 5. GET /auth/kakao/callback?code=...│
     │<───────────────────────────│                              │
     │                            │                              │
     │                            │ 6. POST /oauth/token (code)   │
     │                            │─────────────────────────────>│
     │                            │                              │
     │                            │ 7. { "access_token": "..." }  │
     │                            │<──────────────────────────────│
     │                            │                              │
     │                            │ 8. GET /v2/user/me (token)    │
     │                            │─────────────────────────────>│
     │                            │                              │
     │                            │ 9. { "id": 123, ... }         │
     │                            │<──────────────────────────────│
     │                            │                              │
     │ 10. Redirect to /auth/kakao/success?token=...             │
     │<───────────────────────────│                              │
```

### 6.2 데이터 형식 변환

#### 프론트엔드 → 백엔드
- **형식**: JSON
- **예시**:
  ```json
  { "code": "AUTHORIZATION_CODE" }
  ```
- **Spring Boot 처리**: `@RequestBody Map<String, String>`로 자동 변환

#### 백엔드 → 카카오/네이버
- **카카오**: Form URL Encoded
  ```
  grant_type=authorization_code&client_id=...&code=...
  ```
- **네이버**: URL 쿼리 파라미터
  ```
  ?grant_type=authorization_code&client_id=...&code=...
  ```

#### 카카오/네이버 → 백엔드
- **형식**: JSON
- **Spring Boot 처리**: `RestTemplate`이 자동으로 DTO로 변환

#### 백엔드 → 프론트엔드
- **형식**: JSON 또는 URL 쿼리 파라미터 (리다이렉트)
- **JSON 예시**:
  ```json
  {
    "success": true,
    "token": "JWT_TOKEN",
    "user": {
      "id": 123456789,
      "email": "user@example.com",
      "nickname": "사용자"
    }
  }
  ```

---

## 7. 주요 차이점 비교

### 7.1 카카오 vs 네이버 비교표

| 항목 | 카카오 | 네이버 |
|------|--------|--------|
| **Authorization URL** | `https://kauth.kakao.com/oauth/authorize` | `https://nid.naver.com/oauth2.0/authorize` |
| **Token URL** | `https://kauth.kakao.com/oauth/token` | `https://nid.naver.com/oauth2.0/token` |
| **User Info URL** | `https://kapi.kakao.com/v2/user/me` | `https://openapi.naver.com/v1/nid/me` |
| **Token 요청 방식** | POST (Form URL Encoded) | GET (URL 파라미터) |
| **Client Secret** | 불필요 | 필수 |
| **State 파라미터** | 선택적 | 필수 (CSRF 방지) |
| **사용자 ID 타입** | Long | String |
| **Scope 설정** | `profile_nickname,profile_image` | 기본 제공 (별도 설정 불필요) |
| **에러 처리** | HTTP 상태 코드 | `resultcode` 필드 ("00" = 성공) |

### 7.2 코드 구조 비교

#### 카카오 Access Token 요청
```java
// POST 요청, Form URL Encoded
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
params.add("grant_type", "authorization_code");
params.add("client_id", kakaoConfig.getRestApiKey());
params.add("redirect_uri", kakaoConfig.getRedirectUri());
params.add("code", code);

HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
    KAKAO_TOKEN_URL, request, KakaoTokenResponse.class
);
```

#### 네이버 Access Token 요청
```java
// GET 요청, URL 파라미터
String url = String.format(
    "%s?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&state=%s",
    NAVER_TOKEN_URL,
    naverConfig.getClientId(),
    naverConfig.getClientSecret(),
    code,
    state != null ? state : ""
);

ResponseEntity<NaverTokenResponse> response = restTemplate.getForEntity(
    url, NaverTokenResponse.class
);
```

---

## 8. 실전 팁과 주의사항

### 8.1 보안 관련

#### 1. Client Secret 관리
- ✅ `.env` 파일에 저장 (Git에 커밋하지 않기)
- ✅ 환경 변수로 주입
- ❌ 코드에 하드코딩하지 않기

#### 2. State 파라미터 (CSRF 방지)
- 네이버는 **반드시** state 파라미터 사용
- 카카오는 선택적이지만 **권장**
- State는 세션에 저장하고 검증해야 함 (현재 구현은 간단한 타임스탬프 사용)

#### 3. Redirect URI 검증
- 카카오/네이버 개발자 콘솔에 등록한 Redirect URI와 정확히 일치해야 함
- URL 인코딩 주의

### 8.2 URL 인코딩

**중요**: Redirect URI와 State는 반드시 URL 인코딩해야 함

```java
// 올바른 방법
String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8.toString());

// 잘못된 방법 (인코딩 안 함)
String url = baseUrl + "?redirect_uri=" + redirectUri;  // ❌
```

### 8.3 에러 처리

#### 카카오 에러 처리
```java
try {
    ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(...);
    // 성공
} catch (HttpClientErrorException e) {
    // HTTP 4xx 에러
    System.err.println("에러: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
}
```

#### 네이버 에러 처리
```java
NaverTokenResponse tokenResponse = response.getBody();
if (tokenResponse.getError() != null) {
    // 네이버 API 에러 응답
    throw new RuntimeException("에러: " + tokenResponse.getError());
}

NaverUserInfo userInfo = response.getBody();
if (!"00".equals(userInfo.getResultCode())) {
    // 네이버 API 에러 응답
    throw new RuntimeException("에러: " + userInfo.getMessage());
}
```

### 8.4 Gateway 라우팅 설정

**중요**: Gateway에서 콜백 URL 라우팅 설정 필수

```yaml
routes:
  # 카카오 콜백
  - id: kakao-callback
    uri: http://authservice:8081
    predicates:
      - Path=/auth/kakao/callback
    filters:
      - RewritePath=/auth/kakao/callback, /kakao/callback
  
  # 네이버 콜백
  - id: naver-callback
    uri: http://authservice:8081
    predicates:
      - Path=/auth/naver/callback
    filters:
      - RewritePath=/auth/naver/callback, /naver/callback
```

### 8.5 CORS 설정

Gateway에서 CORS 설정 필수:

```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins:
        - "http://localhost:3000"
      allowedMethods:
        - GET
        - POST
        - OPTIONS
      allowedHeaders: "*"
      allowCredentials: true
```

### 8.6 네이버 ID 처리

네이버는 사용자 ID가 String이므로 Long으로 변환 필요:

```java
// 해시코드로 변환 (간단한 방법)
Long userId = Long.valueOf(userInfo.getExtractedId().hashCode());

// 또는 UUID를 Long으로 변환하는 더 안전한 방법 사용 가능
```

### 8.7 디버깅 팁

1. **로그 확인**
   - 각 단계마다 System.out.println으로 로그 출력
   - 에러 발생 시 스택 트레이스 확인

2. **API 응답 확인**
   - RestTemplate의 응답 본문 확인
   - 카카오/네이버 개발자 콘솔에서 API 호출 이력 확인

3. **네트워크 탭 확인**
   - 브라우저 개발자 도구의 Network 탭에서 요청/응답 확인
   - 헤더, 바디, 상태 코드 확인

---

## 9. 전체 코드 구조

### 9.1 디렉토리 구조

```
services/authservice/src/main/java/kr/ai/kjun/api/
├── config/
│   ├── KakaoConfig.java          # 카카오 설정
│   ├── NaverConfig.java          # 네이버 설정
│   └── RestTemplateConfig.java   # RestTemplate 빈 설정
├── kakao/
│   ├── KakaoController.java      # 카카오 컨트롤러
│   ├── KakaoService.java         # 카카오 서비스
│   └── dto/
│       ├── KakaoTokenResponse.java
│       └── KakaoUserInfo.java
├── naver/
│   ├── NaverController.java      # 네이버 컨트롤러
│   ├── NaverService.java         # 네이버 서비스
│   └── dto/
│       ├── NaverTokenResponse.java
│       └── NaverUserInfo.java
└── jwt/
    ├── JwtTokenProvider.java    # JWT 토큰 생성/검증
    └── JwtProperties.java        # JWT 설정
```

### 9.2 주요 클래스 역할

#### Controller
- **역할**: HTTP 요청/응답 처리
- **책임**:
  - 프론트엔드 요청 받기
  - Service 호출
  - 응답 생성 및 반환

#### Service
- **역할**: 비즈니스 로직 처리
- **책임**:
  - OAuth API 호출
  - 사용자 정보 추출
  - 에러 처리

#### DTO (Data Transfer Object)
- **역할**: API 응답 데이터 구조화
- **책임**:
  - JSON 응답을 Java 객체로 변환
  - 편의 메서드 제공 (getExtractedEmail 등)

#### Config
- **역할**: 설정 관리
- **책임**:
  - 환경 변수 바인딩
  - RestTemplate 빈 생성

---

## 10. 학습 체크리스트

### 기본 개념 이해
- [ ] OAuth 2.0의 기본 개념 이해
- [ ] Authorization Code Flow 이해
- [ ] JWT 토큰의 구조와 용도 이해

### 구현 이해
- [ ] 카카오 OAuth 전체 흐름 이해
- [ ] 네이버 OAuth 전체 흐름 이해
- [ ] 카카오와 네이버의 차이점 이해
- [ ] JWT 토큰 생성 과정 이해

### 코드 이해
- [ ] Controller의 역할 이해
- [ ] Service의 역할 이해
- [ ] DTO의 역할 이해
- [ ] Config의 역할 이해

### 실전 적용
- [ ] 환경 변수 설정 방법 이해
- [ ] Gateway 라우팅 설정 이해
- [ ] 에러 처리 방법 이해
- [ ] 보안 주의사항 이해

---

## 11. 추가 학습 자료

### 공식 문서
- [OAuth 2.0 공식 문서](https://oauth.net/2/)
- [카카오 로그인 API 문서](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)
- [네이버 로그인 API 문서](https://developers.naver.com/docs/login/overview/)

### Spring Boot 관련
- [Spring Boot RestTemplate 가이드](https://www.baeldung.com/rest-template)
- [Spring Cloud Gateway 가이드](https://spring.io/projects/spring-cloud-gateway)

### JWT 관련
- [JWT 공식 사이트](https://jwt.io/)
- [JJWT 라이브러리 문서](https://github.com/jwtk/jjwt)

---

## 12. 마무리

이 가이드를 통해 OAuth 2.0의 전체 흐름과 구현 방법을 이해할 수 있습니다. 

**핵심 요약**:
1. OAuth 2.0은 Authorization Code Flow를 사용
2. 카카오와 네이버는 API 호출 방식이 다름
3. JWT 토큰으로 사용자 인증 정보 전달
4. 보안을 위해 환경 변수 사용, URL 인코딩 필수

**다음 단계**:
- 실제 프로젝트에 적용해보기
- 에러 케이스 처리 추가
- State 검증 로직 강화
- Refresh Token 처리 추가

---

**작성일**: 2024년
**버전**: 1.0
**작성자**: AI Assistant

