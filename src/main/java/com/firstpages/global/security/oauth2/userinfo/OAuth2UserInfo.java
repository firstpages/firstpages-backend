package com.firstpages.global.security.oauth2.userinfo;

import com.firstpages.domain.user.entity.SocialProvider;

// 소셜 로그인 사용자 정보의 공통 규약
public interface OAuth2UserInfo {

    // 소셜 로그인 제공자를 반환
    SocialProvider getProvider();

    // 소셜 로그인 제공자가 발급한 사용자 고유 식별자를 반환
    String getProviderId();
}
