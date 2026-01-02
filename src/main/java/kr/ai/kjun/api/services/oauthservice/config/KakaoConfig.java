package kr.ai.kjun.api.services.oauthservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 카카오 OAuth 설정
 * application.yaml의 kakao.* 값을 읽어옴
 */
@Component
@ConfigurationProperties(prefix = "kakao")
public class KakaoConfig {

    private String restApiKey;
    private String redirectUri;

    // Getters and Setters
    public String getRestApiKey() {
        return restApiKey;
    }

    public void setRestApiKey(String restApiKey) {
        this.restApiKey = restApiKey;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
