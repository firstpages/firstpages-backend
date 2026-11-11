package com.firstpages.global.security.oauth2;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final Long userId;

    private final Map<String, Object> attributes;

    private final Collection<? extends GrantedAuthority> authorities;

    public CustomOAuth2User(
            Long userId,
            OAuth2User oauth2User
    ) {
        this.userId = userId;
        this.attributes = oauth2User.getAttributes();
        this.authorities = oauth2User.getAuthorities();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
