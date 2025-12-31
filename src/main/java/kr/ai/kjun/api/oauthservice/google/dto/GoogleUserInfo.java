package kr.ai.kjun.api.oauthservice.google.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 구글 사용자 정보 응답 DTO
 * https://www.googleapis.com/oauth2/v2/userinfo 응답
 */
public class GoogleUserInfo {

    private String id;

    private String email;

    @JsonProperty("verified_email")
    private Boolean verifiedEmail;

    private String name;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    private String picture;

    private String locale;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getVerifiedEmail() {
        return verifiedEmail;
    }

    public void setVerifiedEmail(Boolean verifiedEmail) {
        this.verifiedEmail = verifiedEmail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * 간소화된 이메일 추출 (없으면 기본값 반환)
     */
    public String getExtractedEmail() {
        return email != null ? email : "no-email@gmail.com";
    }

    /**
     * 간소화된 닉네임 추출 (없으면 기본값 반환)
     */
    public String getExtractedNickname() {
        return name != null ? name : "구글 사용자";
    }

    /**
     * 간소화된 프로필 이미지 URL 추출 (없으면 null 반환)
     */
    public String getExtractedProfileImage() {
        return picture;
    }

    /**
     * 사용자 ID를 Long으로 변환 (해시코드 사용)
     */
    public Long getExtractedIdAsLong() {
        return id != null ? Long.valueOf(id.hashCode()) : 0L;
    }
}

