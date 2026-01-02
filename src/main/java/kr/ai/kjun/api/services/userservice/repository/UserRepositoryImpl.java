package kr.ai.kjun.api.services.userservice.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import kr.ai.kjun.api.services.userservice.entity.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository 커스텀 구현체
 * 복잡한 쿼리나 QueryDSL을 사용하는 메서드를 구현
 * 
 * 현재는 JPA EntityManager를 사용하지만,
 * 나중에 QueryDSL을 추가할 수 있도록 구조를 준비
 */
@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final EntityManager entityManager;

    public UserRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * OAuth 제공자와 OAuth ID로 사용자 조회 (커스텀 구현)
     * 
     * @param oauthProvider OAuth 제공자
     * @param oauthId       OAuth ID
     * @return 사용자 정보
     */
    @Override
    public Optional<User> findByOauthProviderAndOauthIdCustom(User.OAuthProvider oauthProvider, String oauthId) {
        String jpql = "SELECT u FROM User u WHERE u.oauthProvider = :oauthProvider AND u.oauthId = :oauthId";
        TypedQuery<User> query = entityManager.createQuery(jpql, User.class);
        query.setParameter("oauthProvider", oauthProvider);
        query.setParameter("oauthId", oauthId);

        try {
            User user = query.getSingleResult();
            return Optional.of(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 이메일로 사용자 조회 (커스텀 구현)
     * 
     * @param email 이메일 주소
     * @return 사용자 정보
     */
    @Override
    public Optional<User> findByEmailCustom(String email) {
        String jpql = "SELECT u FROM User u WHERE u.email = :email";
        TypedQuery<User> query = entityManager.createQuery(jpql, User.class);
        query.setParameter("email", email);

        try {
            User user = query.getSingleResult();
            return Optional.of(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
