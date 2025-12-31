package kr.ai.kjun.api.userservice.repository;

import kr.ai.kjun.api.userservice.entity.User;

import java.util.Optional;

/**
 * User Repository 커스텀 인터페이스
 * 복잡한 쿼리나 QueryDSL을 사용하는 메서드를 정의
 */
public interface UserRepositoryCustom {

    /**
     * OAuth 제공자와 OAuth ID로 사용자 조회 (커스텀 구현 가능)
     * 
     * @param oauthProvider OAuth 제공자
     * @param oauthId       OAuth ID
     * @return 사용자 정보
     */
    Optional<User> findByOauthProviderAndOauthIdCustom(User.OAuthProvider oauthProvider, String oauthId);

    /**
     * 이메일로 사용자 조회 (커스텀 구현 가능)
     * 
     * @param email 이메일 주소
     * @return 사용자 정보
     */
    Optional<User> findByEmailCustom(String email);
}
