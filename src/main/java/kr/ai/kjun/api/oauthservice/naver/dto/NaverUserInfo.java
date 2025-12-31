package kr.ai.kjun.api.oauthservice.naver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 네이버 사용자 정보 응답 DTO
 * https://openapi.naver.com/v1/nid/me 응답
 */
public class NaverUserInfo {

    @JsonProperty("resultcode")
    private String resultCode;

    private String message;

    private Response response;

    // Getters and Setters
    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    /**
     * 간소화된 이메일 추출 (없으면 기본값 반환)
     */
    public String getExtractedEmail() {
        return response != null && response.getEmail() != null
                ? response.getEmail()
                : "no-email@naver.com";
    }

    /**
     * 간소화된 닉네임 추출 (없으면 기본값 반환)
     */
    public String getExtractedNickname() {
        return response != null && response.getNickname() != null
                ? response.getNickname()
                : response != null && response.getName() != null
                        ? response.getName()
                        : "네이버 사용자";
    }

    /**
     * 간소화된 프로필 이미지 URL 추출 (없으면 null 반환)
     */
    public String getExtractedProfileImage() {
        return response != null ? response.getProfileImage() : null;
    }

    /**
     * 네이버 사용자 ID 추출 (없으면 null 반환)
     */
    public String getExtractedId() {
        return response != null ? response.getId() : null;
    }

    /**
     * 네이버 사용자 정보 응답
     */
    public static class Response {

        private String id;
        private String nickname;
        private String name;
        private String email;
        private String gender;
        private String age;
        private String birthday;

        @JsonProperty("profile_image")
        private String profileImage;

        private String birthyear;
        private String mobile;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getAge() {
            return age;
        }

        public void setAge(String age) {
            this.age = age;
        }

        public String getBirthday() {
            return birthday;
        }

        public void setBirthday(String birthday) {
            this.birthday = birthday;
        }

        public String getProfileImage() {
            return profileImage;
        }

        public void setProfileImage(String profileImage) {
            this.profileImage = profileImage;
        }

        public String getBirthyear() {
            return birthyear;
        }

        public void setBirthyear(String birthyear) {
            this.birthyear = birthyear;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }
    }
}

