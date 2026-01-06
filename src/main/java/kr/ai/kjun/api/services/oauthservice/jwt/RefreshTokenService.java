package kr.ai.kjun.api.services.oauthservice.jwt;

import kr.ai.kjun.api.services.oauthservice.entity.RefreshToken;
import kr.ai.kjun.api.services.oauthservice.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token 서비스
 * Refresh Token을 Neon PostgreSQL에 저장 및 관리
 * OAuth Service에서 직접 관리 (User Service와 분리)
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // Refresh Token 기본 TTL: 7일
    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Refresh Token 생성 및 Neon DB에 저장
     * 
     * @param userId 사용자 ID
     * @return 생성된 Refresh Token
     */
    @Transactional
    public String generateAndSaveRefreshToken(Long userId) {
        // UUID 기반 Refresh Token 생성
        String refreshToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS);

        // 기존 Refresh Token이 있으면 삭제
        refreshTokenRepository.deleteByUserId(userId);

        // 새로운 Refresh Token 저장
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setUserId(userId);
        tokenEntity.setToken(refreshToken);
        tokenEntity.setExpiresAt(expiresAt);
        refreshTokenRepository.save(tokenEntity);

        System.out.println("✅ [Refresh Token] 생성 및 Neon DB 저장 완료 - userId: " + userId);
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
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByUserId(userId);

        if (tokenOpt.isEmpty()) {
            System.out.println("❌ [Refresh Token] 토큰을 찾을 수 없음 - userId: " + userId);
            return false;
        }

        RefreshToken tokenEntity = tokenOpt.get();
        String storedToken = tokenEntity.getToken();
        LocalDateTime expiresAt = tokenEntity.getExpiresAt();

        // 토큰이 일치하지 않으면 실패
        if (!storedToken.equals(refreshToken)) {
            System.out.println("❌ [Refresh Token] 검증 실패 (토큰 불일치) - userId: " + userId);
            return false;
        }

        // 만료 시간 확인
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            System.out.println("❌ [Refresh Token] 검증 실패 (만료됨) - userId: " + userId);
            // 만료된 토큰은 삭제
            refreshTokenRepository.delete(tokenEntity);
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
    @Transactional
    public void deleteRefreshToken(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        System.out.println("🗑️ [Refresh Token] Neon DB에서 삭제 완료 - userId: " + userId);
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
