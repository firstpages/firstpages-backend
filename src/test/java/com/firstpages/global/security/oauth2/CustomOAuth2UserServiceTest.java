
package com.firstpages.global.security.oauth2;

import com.firstpages.domain.user.entity.SocialProvider;
import com.firstpages.domain.user.entity.User;
import com.firstpages.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

// 카카오 로그인 시 사용자 조회 및 생성 로직 검증
class CustomOAuth2UserServiceTest {

    private static final Long USER_ID = 9L;
    private static final Long KAKAO_ID = 123456789L;
    private static final String KAKAO_PROVIDER_ID = "123456789";

    @Test
    @DisplayName("최초 카카오 로그인 시 신규 사용자를 저장한다")
    void createNewUser() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        User savedUser = mock(User.class);

        when(userRepository.findByProviderAndProviderId(
                SocialProvider.KAKAO,
                KAKAO_PROVIDER_ID
        )).thenReturn(Optional.empty());

        // DB 저장 후 사용자 ID가 부여된 엔티티를 반환하도록 설정
        when(savedUser.getId()).thenReturn(USER_ID);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        // when
        CustomOAuth2User principal = loadKakaoUser(userRepository);

        // then
        assertEquals(USER_ID, principal.getUserId());

        // 카카오 사용자 ID를 이용해 기존 사용자를 검색했는지 검증
        verify(userRepository).findByProviderAndProviderId(
                SocialProvider.KAKAO,
                KAKAO_PROVIDER_ID
        );

        // 신규 사용자를 한 번만 저장했는지 검증
        verify(userRepository, times(1)).save(
                argThat(user ->
                        user.getProvider() == SocialProvider.KAKAO
                                && KAKAO_PROVIDER_ID.equals(
                                user.getProviderId()
                        )
                )
        );
    }

    @Test
    @DisplayName("기존 카카오 사용자는 새로 저장하지 않는다")
    void loginExistingUser() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        User existingUser = mock(User.class);

        when(existingUser.getId()).thenReturn(USER_ID);

        when(userRepository.findByProviderAndProviderId(
                SocialProvider.KAKAO,
                KAKAO_PROVIDER_ID
        )).thenReturn(Optional.of(existingUser));

        // when
        CustomOAuth2User principal = loadKakaoUser(userRepository);

        // then
        assertEquals(USER_ID, principal.getUserId());

        // 기존 회원은 DB에 다시 INSERT하지 않아야 함
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("기존 사용자의 DB ID를 인증 Principal에 사용한다")
    void principalUsesDatabaseUserId() {
        // given
        UserRepository userRepository = mock(UserRepository.class);
        User existingUser = mock(User.class);

        when(existingUser.getId()).thenReturn(USER_ID);

        when(userRepository.findByProviderAndProviderId(
                SocialProvider.KAKAO,
                KAKAO_PROVIDER_ID
        )).thenReturn(Optional.of(existingUser));

        // when
        CustomOAuth2User principal = loadKakaoUser(userRepository);

        // then
        // 카카오 ID가 아닌 FirstPages DB 사용자 ID여야 함
        assertEquals(9L, principal.getUserId());
        assertNotEquals(KAKAO_ID, principal.getUserId());
    }

    // 실제 카카오 API 호출 대신 가짜 OAuth2 사용자 응답을 사용
    private CustomOAuth2User loadKakaoUser(
            UserRepository userRepository
    ) {
        OAuth2User kakaoUser = createKakaoOAuth2User();

        // Spring Security 기본 OAuth2 사용자 조회를 Mock으로 대체
        try (var mocked = mockConstruction(
                DefaultOAuth2UserService.class,
                (mock, context) -> when(
                        mock.loadUser(any(OAuth2UserRequest.class))
                ).thenReturn(kakaoUser)
        )) {
            CustomOAuth2UserService service =
                    new CustomOAuth2UserService(userRepository);

            OAuth2User result = service.loadUser(
                    createOAuth2UserRequest()
            );

            assertInstanceOf(CustomOAuth2User.class, result);

            return (CustomOAuth2User) result;
        }
    }

    // 카카오 사용자 정보 API의 응답 형태 생성
    private OAuth2User createKakaoOAuth2User() {
        Map<String, Object> profile = Map.of(
                "nickname", "테스트사용자"
        );

        Map<String, Object> kakaoAccount = Map.of(
                "profile", profile
        );

        Map<String, Object> attributes = Map.of(
                "id", KAKAO_ID,
                "kakao_account", kakaoAccount
        );

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
    }

    // 테스트용 카카오 OAuth2 요청 생성
    private OAuth2UserRequest createOAuth2UserRequest() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("kakao")
                .clientId("test-kakao-client")
                .clientSecret("test-kakao-secret")
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE
                )
                .redirectUri(
                        "{baseUrl}/login/oauth2/code/{registrationId}"
                )
                .authorizationUri(
                        "https://kauth.kakao.com/oauth/authorize"
                )
                .tokenUri(
                        "https://kauth.kakao.com/oauth/token"
                )
                .userInfoUri(
                        "https://kapi.kakao.com/v2/user/me"
                )
                .userNameAttributeName("id")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-kakao-access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OAuth2UserRequest(registration, accessToken);
    }
}
