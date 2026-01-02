package kr.ai.kjun.api.services.userservice.service;

import kr.ai.kjun.api.services.userservice.entity.User;
import kr.ai.kjun.api.services.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자 서비스 구현체
 * 사용자 정보 저장 및 조회 로직 구현
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    /**
     * OAuth 제공자와 OAuth ID로 사용자 조회
     * 
     * @param oauthProvider OAuth 제공자
     * @param oauthId       OAuth ID
     * @return 사용자 정보 (없으면 Optional.empty())
     */
    @Override
    public Optional<User> findByOAuthProviderAndOAuthId(User.OAuthProvider oauthProvider, String oauthId) {
        return userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId);
    }

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
    @Override
    @Transactional
    public User saveOrUpdateUser(
            User.OAuthProvider oauthProvider,
            String oauthId,
            String email,
            String nickname,
            String profileImageUrl) {

        Optional<User> existingUser = userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId);

        if (existingUser.isPresent()) {
            // 기존 사용자 업데이트
            User user = existingUser.get();
            user.setEmail(email);
            user.setNickname(nickname);
            if (profileImageUrl != null) {
                user.setProfileImageUrl(profileImageUrl);
            }
            return userRepository.save(user);
        } else {
            // 새 사용자 생성
            User newUser = new User();
            newUser.setOauthProvider(oauthProvider);
            newUser.setOauthId(oauthId);
            newUser.setEmail(email);
            newUser.setNickname(nickname);
            newUser.setProfileImageUrl(profileImageUrl);
            newUser.setRole(User.UserRole.USER);
            return userRepository.save(newUser);
        }
    }

    /**
     * 사용자 ID로 사용자 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보 (없으면 Optional.empty())
     */
    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }
}
