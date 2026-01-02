package kr.ai.kjun.api.services.oauthservice.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Token Blacklist 서비스
 * 로그아웃된 Access Token을 Redis에 저장하여 Gateway에서 검증
 * Key 네이밍: auth:blacklist:{jti}
 */
@Service
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    // Redis Key 접두사
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token을 블랙리스트에 추가
     * 
     * @param accessToken 블랙리스트에 추가할 Access Token
     */
    public void addToBlacklist(String accessToken) {
        try {
            // JWT에서 Claims 추출
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            // JWT ID (jti) 추출
            String jti = claims.getId();
            if (jti == null) {
                // jti가 없으면 subject를 사용
                jti = claims.getSubject();
            }

            // 만료 시간 계산
            Date expiration = claims.getExpiration();
            long ttlSeconds = expiration != null
                    ? (expiration.getTime() - System.currentTimeMillis()) / 1000
                    : jwtProperties.getExpiration() / 1000; // 기본 TTL

            if (ttlSeconds > 0) {
                // Redis에 저장 (Key: auth:blacklist:{jti}, Value: "true", TTL: 토큰 만료 시간까지)
                String key = BLACKLIST_PREFIX + jti;
                redisTemplate.opsForValue().set(key, "true", ttlSeconds, TimeUnit.SECONDS);
                System.out.println("✅ [Token Blacklist] 추가 완료 - jti: " + jti);
            }
        } catch (Exception e) {
            System.err.println("❌ [Token Blacklist] 추가 실패: " + e.getMessage());
        }
    }

    /**
     * Access Token이 블랙리스트에 있는지 확인
     * 
     * @param accessToken 확인할 Access Token
     * @return 블랙리스트에 있으면 true, 없으면 false
     */
    public boolean isBlacklisted(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            String jti = claims.getId();
            if (jti == null) {
                jti = claims.getSubject();
            }

            String key = BLACKLIST_PREFIX + jti;
            String value = redisTemplate.opsForValue().get(key);
            return value != null && !value.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
