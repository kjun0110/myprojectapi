package kr.ai.kjun.api.services.oauthservice.jwt;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 서비스
 * Refresh Token을 Upstash Redis에 저장 및 관리
 * Key 네이밍: auth:refresh:{userId}
 */
@Service
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    // Redis Key 접두사
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    // Refresh Token 기본 TTL: 7일
    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    public RefreshTokenService(RedisTemplate<String, String> redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Refresh Token 생성 및 Redis에 저장
     * 
     * @param userId 사용자 ID
     * @return 생성된 Refresh Token
     */
    public String generateAndSaveRefreshToken(Long userId) {
        // UUID 기반 Refresh Token 생성
        String refreshToken = UUID.randomUUID().toString();

        // Redis에 저장 (Key: auth:refresh:{userId}, Value: refreshToken, TTL: 7일)
        String key = REFRESH_TOKEN_PREFIX + userId;
        long ttlSeconds = TimeUnit.DAYS.toSeconds(REFRESH_TOKEN_TTL_DAYS);

        redisTemplate.opsForValue().set(key, refreshToken, ttlSeconds, TimeUnit.SECONDS);

        System.out.println("✅ [Refresh Token] 생성 및 저장 완료 - userId: " + userId);
        return refreshToken;
    }

    /**
     * Refresh Token 검증
     * 
     * @param userId       사용자 ID
     * @param refreshToken 검증할 Refresh Token
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            System.out.println("❌ [Refresh Token] 검증 실패 - userId: " + userId);
            return false;
        }

        System.out.println("✅ [Refresh Token] 검증 성공 - userId: " + userId);
        return true;
    }

    /**
     * Refresh Token 삭제 (로그아웃 시 사용)
     * 
     * @param userId 사용자 ID
     */
    public void deleteRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        System.out.println("🗑️ [Refresh Token] 삭제 완료 - userId: " + userId);
    }

    /**
     * Refresh Token 갱신 (기존 토큰 삭제 후 새로 생성)
     * 
     * @param userId 사용자 ID
     * @return 새로운 Refresh Token
     */
    public String refreshToken(Long userId) {
        deleteRefreshToken(userId);
        return generateAndSaveRefreshToken(userId);
    }
}
