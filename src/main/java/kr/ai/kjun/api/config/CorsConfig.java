package kr.ai.kjun.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 설정
 * 프론트엔드에서의 요청 허용
 * - 개발 환경: localhost:3000, localhost:4000
 * - 프로덕션 환경: https://www.kjun.ai.kr, https://kjun.ai.kr
 * - Vercel 프리뷰: https://*.vercel.app (와일드카드는 직접 지원 안 되므로 개별 추가 필요)
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:4000",
                        "https://www.kjun.ai.kr",
                        "https://kjun.ai.kr"
                // Vercel 프리뷰 도메인은 필요시 개별 추가
                // "https://your-project-name.vercel.app"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
