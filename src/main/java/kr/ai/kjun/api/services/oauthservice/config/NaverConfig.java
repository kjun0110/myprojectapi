package kr.ai.kjun.api.services.oauthservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 네이버 OAuth 설정
 * application.yaml의 naver.* 값을 읽어옴
 */
@Component
@ConfigurationProperties(prefix = "naver")
public class NaverConfig {
    
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    // Getters and Setters
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}

