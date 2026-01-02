package kr.ai.kjun.api.services.oauthservice.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 토큰 생성 및 검증 (WebFlux 없이)
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // HMAC-SHA 알고리즘을 위한 SecretKey 생성
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWT 토큰 생성
     * 
     * @param userId   사용자 ID
     * @param email    이메일
     * @param nickname 닉네임
     * @return JWT 토큰
     */
    public String generateToken(Long userId, String email, String nickname) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("nickname", nickname);

        // JWT ID (jti) 생성 - 블랙리스트 관리에 사용
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .claims(claims)
                .id(jti) // JWT ID 추가
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * JWT 토큰 검증
     * 
     * @param token JWT 토큰
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            System.err.println("❌ JWT 토큰 검증 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * JWT 토큰에서 모든 Claims 추출
     * 
     * @param token JWT 토큰
     * @return Claims
     */
    public Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
