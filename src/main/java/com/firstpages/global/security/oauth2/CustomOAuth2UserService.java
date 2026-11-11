package com.firstpages.global.security.oauth2;

import com.firstpages.domain.user.entity.SocialProvider;
import com.firstpages.domain.user.entity.User;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.security.oauth2.userinfo.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    // 카카오 사용자 정보 조회 및 회원 처리
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 카카오 사용자 정보 API 호출
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        Map<String, Object> attributes = oauth2User.getAttributes();

        // 카카오 사용자 고유 ID 추출
        String providerId = extractProviderId(attributes);

        // 카카오 프로필에서 닉네임 추출
        String nickname = extractNickname(attributes);

        User user = userRepository
                .findByProviderAndProviderId(
                        SocialProvider.KAKAO,
                        providerId
                )
                .orElseGet(() -> userRepository.save(
                        User.create(
                                SocialProvider.KAKAO,
                                providerId,
                                nickname
                        )
                ));

        // 기존 사용자의 닉네임이 없다면 최초 한 번 보완
        if (user.getNickname() == null && nickname != null) {
            user.updateNickname(nickname);
        }

        return new CustomOAuth2User(
                user.getId(),
                oauth2User
        );
    }

    // 카카오 사용자 ID 추출
    private String extractProviderId(
            Map<String, Object> attributes
    ) {
        Object id = attributes.get("id");

        if (!(id instanceof Number) && !(id instanceof String)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_kakao_user_id")
            );
        }

        String providerId = id.toString();

        if (providerId.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_kakao_user_id")
            );
        }

        return providerId;
    }

    // 카카오 사용자 닉네임 추출
    private String extractNickname(
            Map<String, Object> attributes
    ) {
        Object accountObject = attributes.get("kakao_account");

        if (!(accountObject instanceof Map<?, ?> account)) {
            return null;
        }

        Object profileObject = account.get("profile");

        if (!(profileObject instanceof Map<?, ?> profile)) {
            return null;
        }

        Object nicknameObject = profile.get("nickname");

        if (!(nicknameObject instanceof String nickname)) {
            return null;
        }

        String result = nickname.trim();

        return result.isEmpty() ? null : result;
    }
}
