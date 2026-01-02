package kr.ai.kjun.api.services.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 Model (DTO/VO)
 * API 요청/응답에 사용되는 데이터 전송 객체
 * Entity와 분리하여 사용
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserModel {

    private Long id;
    private String email;
    private String oauthProvider; // KAKAO, NAVER, GOOGLE
    private String oauthId;
    private String nickname;
    private String profileImageUrl;
    private String role; // USER, ADMIN
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
