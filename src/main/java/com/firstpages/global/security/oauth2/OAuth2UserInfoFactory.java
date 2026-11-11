package com.firstpages.global.security.oauth2;

import com.firstpages.global.security.oauth2.userinfo.KakaoUserInfo;
import com.firstpages.global.security.oauth2.userinfo.OAuth2UserInfo;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

// OAuth2 로그인 제공자에 따라 사용자 정보 구현체를 생성하는 팩토리 클래스
public final class OAuth2UserInfoFactory {

    private static final String KAKAO_REGISTRATION_ID = "kakao";

    // 외부 인스턴스 생성 방지
    private OAuth2UserInfoFactory() {
    }

    // OAuth2 제공자에 대응하는 사용자 정보 객체를 생성
    public static OAuth2UserInfo create(
            String registrationId,
            Map<String, Object> attributes
    ) {
        if (KAKAO_REGISTRATION_ID.equals(registrationId)) {
            return new KakaoUserInfo(attributes);
        }

        throw new OAuth2AuthenticationException(
                new OAuth2Error("unsupported_provider"),
                "지원하지 않는 소셜 로그인 제공자입니다."
        );
    }
}
