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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    // 카카오 사용자 정보 조회 및 회원 처리
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) {
        // 카카오 사용자 정보 조회와 회원번호 추출
        OAuth2User oAuth2User = delegate.loadUser(request);
        String providerId = extractProviderId(oAuth2User.getAttributes());

        Optional<User> existing = userRepository.findByProviderAndProviderIdForUpdate(
                SocialProvider.KAKAO,
                providerId
        );

        User user;

        if (existing.isPresent()) {
            user = existing.get();
            String nickname = extractNickname(oAuth2User.getAttributes());

            if (user.getNickname() == null && nickname != null) {
                user.updateNickname(nickname);
            }
        } else {
            // 회원이 사라진 경우 기존 카카오 토큰으로 사용자 정보 재확인
            oAuth2User = delegate.loadUser(request);

            // 최초 조회와 재조회 응답의 카카오 회원번호 비교
            if (!providerId.equals(extractProviderId(oAuth2User.getAttributes()))) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_kakao_user_id"));
            }

            // 검증된 카카오 정보로 새 내부 회원 생성
            user = userRepository.save(User.create(
                    SocialProvider.KAKAO,
                    providerId,
                    extractNickname(oAuth2User.getAttributes())
            ));
        }

        // 내부 회원 ID를 가진 OAuth 인증 주체 반환
        return new CustomOAuth2User(
                user.getId(),
                oAuth2User
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
