package kr.ai.kjun.api.services.userservice.service;

import kr.ai.kjun.api.services.userservice.entity.User;

import java.util.Optional;

/**
 * 사용자 서비스 인터페이스
 * 사용자 정보 저장 및 조회 로직
 */
public interface UserService {

    /**
     * OAuth 제공자와 OAuth ID로 사용자 조회
     * 
     * @param oauthProvider OAuth 제공자
     * @param oauthId       OAuth ID
     * @return 사용자 정보 (없으면 Optional.empty())
     */
    Optional<User> findByOAuthProviderAndOAuthId(User.OAuthProvider oauthProvider, String oauthId);

    /**
     * OAuth 로그인 사용자 저장 또는 업데이트
     * 기존 사용자가 있으면 업데이트, 없으면 새로 생성
     * 
     * @param oauthProvider   OAuth 제공자
     * @param oauthId         OAuth ID
     * @param email           이메일
     * @param nickname        닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @return 저장된 사용자 정보
     */
    User saveOrUpdateUser(
            User.OAuthProvider oauthProvider,
            String oauthId,
            String email,
            String nickname,
            String profileImageUrl);

    /**
     * 사용자 ID로 사용자 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보 (없으면 Optional.empty())
     */
    Optional<User> findById(Long userId);
}
