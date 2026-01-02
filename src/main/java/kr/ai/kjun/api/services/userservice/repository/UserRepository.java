package kr.ai.kjun.api.services.userservice.repository;

import kr.ai.kjun.api.services.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 Repository
 * Neon DB에서 사용자 정보 조회 및 저장
 * 
 * Spring Data JPA 기본 메서드 + 커스텀 메서드 사용
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    /**
     * OAuth 제공자와 OAuth ID로 사용자 조회
     * Spring Data JPA 메서드 쿼리 사용
     * 
     * @param oauthProvider OAuth 제공자 (KAKAO, NAVER, GOOGLE)
     * @param oauthId       OAuth 제공자에서 받은 사용자 ID
     * @return 사용자 정보 (없으면 Optional.empty())
     */
    Optional<User> findByOauthProviderAndOauthId(User.OAuthProvider oauthProvider, String oauthId);

    /**
     * 이메일로 사용자 조회
     * Spring Data JPA 메서드 쿼리 사용
     * 
     * @param email 이메일 주소
     * @return 사용자 정보 (없으면 Optional.empty())
     */
    Optional<User> findByEmail(String email);
}
