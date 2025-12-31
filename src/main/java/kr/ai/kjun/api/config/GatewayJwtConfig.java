package kr.ai.kjun.api.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 설정
 * JWT Secret Key 생성
 */
@Configuration
public class GatewayJwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * JWT Secret Key 생성
     */
    @Bean
    public SecretKey jwtSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
