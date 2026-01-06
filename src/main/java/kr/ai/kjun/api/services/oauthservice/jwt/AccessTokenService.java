package kr.ai.kjun.api.services.oauthservice.jwt;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Access Token 서비스
 * Access Token을 Upstash Redis에 저장 및 관리
 * Key 네이밍: auth:access:{userId}
 */
@Service
public class AccessTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    // Redis Key 접두사
    private static final String ACCESS_TOKEN_PREFIX = "auth:access:";

    public AccessTokenService(RedisTemplate<String, String> redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Access Token을 Redis에 저장
     * 
     * @param userId      사용자 ID
     * @param accessToken Access Token (JWT)
     */
    public void saveAccessToken(Long userId, String accessToken) {
        String key = ACCESS_TOKEN_PREFIX + userId;
        // JWT 만료 시간을 TTL로 사용 (밀리초를 초로 변환)
        long ttlSeconds = jwtProperties.getExpiration() / 1000;

        redisTemplate.opsForValue().set(key, accessToken, ttlSeconds, TimeUnit.SECONDS);

        System.out.println("✅ [Access Token] 저장 완료 - userId: " + userId);
    }

    /**
     * Access Token 조회
     * 
     * @param userId 사용자 ID
     * @return 저장된 Access Token (없으면 null)
     */
    public String getAccessToken(Long userId) {
        String key = ACCESS_TOKEN_PREFIX + userId;
        String accessToken = redisTemplate.opsForValue().get(key);

        if (accessToken != null) {
            System.out.println("✅ [Access Token] 조회 성공 - userId: " + userId);
        } else {
            System.out.println("⚠️ [Access Token] 조회 실패 (없음) - userId: " + userId);
        }

        return accessToken;
    }

    /**
     * Access Token 검증
     * 
     * @param userId      사용자 ID
     * @param accessToken 검증할 Access Token
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateAccessToken(Long userId, String accessToken) {
        String storedToken = getAccessToken(userId);

        if (storedToken == null || !storedToken.equals(accessToken)) {
            System.out.println("❌ [Access Token] 검증 실패 - userId: " + userId);
            return false;
        }

        System.out.println("✅ [Access Token] 검증 성공 - userId: " + userId);
        return true;
    }

    /**
     * Access Token 삭제 (로그아웃 시 사용)
     * 
     * @param userId 사용자 ID
     */
    public void deleteAccessToken(Long userId) {
        String key = ACCESS_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        System.out.println("🗑️ [Access Token] 삭제 완료 - userId: " + userId);
    }
}

