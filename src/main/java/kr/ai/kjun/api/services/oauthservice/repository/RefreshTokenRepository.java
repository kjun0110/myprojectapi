package kr.ai.kjun.api.services.oauthservice.repository;

import kr.ai.kjun.api.services.oauthservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Refresh Token Repository
 * Neon DB에서 Refresh Token 조회 및 저장
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 사용자 ID로 Refresh Token 조회
     * 
     * @param userId 사용자 ID
     * @return Refresh Token 정보 (없으면 Optional.empty())
     */
    Optional<RefreshToken> findByUserId(Long userId);

    /**
     * 토큰으로 Refresh Token 조회
     * 
     * @param token Refresh Token 문자열
     * @return Refresh Token 정보 (없으면 Optional.empty())
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 ID로 Refresh Token 삭제
     * 
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}

