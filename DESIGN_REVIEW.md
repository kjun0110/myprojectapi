# 토큰 저장 설계 검토

## 요청 사항
- **Access Token**: Upstash Redis에 저장
- **Refresh Token**: Neon PostgreSQL에 저장

## 설계 검토

### ✅ 장점

#### Access Token을 Upstash에 저장
1. **즉시 무효화 가능**: 로그아웃 시 Redis에서 삭제하여 즉시 토큰 무효화
2. **세션 관리 용이**: 활성 세션 추적 가능
3. **성능**: Redis는 빠른 읽기/쓰기 제공
4. **TTL 관리**: 자동 만료 처리

#### Refresh Token을 Neon에 저장
1. **영구 저장**: 데이터베이스 백업/복구 가능
2. **데이터 일관성**: 사용자 정보와 함께 관리
3. **장기 보관**: 필요시 토큰 이력 관리 가능

### ⚠️ 주의사항

#### Access Token을 Upstash에 저장
1. **성능 고려**: 매 요청마다 Redis 조회 필요 (Gateway에서 검증)
2. **JWT의 stateless 장점 상실**: 하지만 세션 관리 장점 획득
3. **Redis 부하**: 많은 사용자일 경우 Redis 메모리 사용량 증가

#### Refresh Token을 Neon에 저장
1. **성능**: Redis보다 느림 (하지만 Refresh Token은 자주 조회하지 않으므로 괜찮음)
2. **TTL 관리**: 데이터베이스에서 만료 시간 관리 필요
3. **인덱싱**: userId로 빠른 조회를 위한 인덱스 필요

## 결론

✅ **구현 가능하며 적절한 설계입니다.**

- Access Token: Upstash Redis (빠른 검증, 즉시 무효화)
- Refresh Token: Neon PostgreSQL (영구 저장, 사용자 정보와 함께 관리)

## 구현 완료 ✅

1. ✅ User 엔티티에 `refreshToken` 필드 추가
   - `refreshToken`: Refresh Token 저장
   - `refreshTokenExpiresAt`: 만료 시간 저장

2. ✅ AccessTokenService 생성 (Upstash Redis)
   - `saveAccessToken()`: Access Token 저장
   - `getAccessToken()`: Access Token 조회
   - `validateAccessToken()`: Access Token 검증
   - `deleteAccessToken()`: Access Token 삭제

3. ✅ RefreshTokenService 수정 (Neon DB 사용)
   - `generateAndSaveRefreshToken()`: Neon DB에 저장
   - `validateRefreshToken()`: Neon DB에서 검증
   - `deleteRefreshToken()`: Neon DB에서 삭제
   - `refreshToken()`: 토큰 갱신

4. ✅ 각 컨트롤러에서 Access Token 저장 로직 추가
   - GoogleController: Access Token 저장 추가
   - KakaoController: Access Token 저장 추가
   - NaverController: Access Token 저장 추가
   - OAuthController: 토큰 갱신 시 Access Token 저장 추가

## 최종 저장 구조

| 항목 | 저장 위치 | Key/필드 | TTL | 용도 |
|------|----------|---------|-----|------|
| Access Token | Upstash Redis | `auth:access:{userId}` | JWT 만료 시간 | 빠른 검증, 즉시 무효화 |
| Refresh Token | Neon PostgreSQL | `refresh_tokens` 테이블 (OAuth Service) | 7일 | 토큰 갱신용 |
| 로그아웃된 Access Token | Upstash Redis | `auth:blacklist:{jti}` | 토큰 만료까지 | 블랙리스트 |
| 사용자 정보 | Neon PostgreSQL | `users` 테이블 (User Service) | 영구 | 사용자 데이터 |

## 아키텍처 변경 사항

### ✅ Refresh Token을 OAuth Service에서 관리
- **이전**: User 엔티티의 `refresh_token` 필드에 저장 (User Service)
- **현재**: `RefreshToken` 엔티티로 별도 테이블 관리 (OAuth Service)
- **장점**: 
  - User Service와 OAuth Service의 책임 분리
  - Refresh Token 관리 로직이 OAuth Service에 집중
  - User 엔티티가 더 단순해짐

### 새로 생성된 파일
1. `RefreshToken.java`: Refresh Token 엔티티 (OAuth Service)
2. `RefreshTokenRepository.java`: Refresh Token Repository (OAuth Service)

