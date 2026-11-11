package com.firstpages.global.security.oauth2.userinfo;

import com.firstpages.domain.user.entity.SocialProvider;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    public String getProviderId() {
        Object id = attributes.get("id");

        if (!(id instanceof Number)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_kakao_user_info"),
                    "카카오 사용자 ID가 올바르지 않습니다."
            );
        }

        return id.toString();
    }
}
