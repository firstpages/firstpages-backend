
package com.firstpages.global.security.oauth2;

import com.firstpages.global.security.jwt.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

// 실제 OAuth2CookieManager Bean의 쿠키 발급 및 삭제 동작 검증
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class OAuth2CookieManagerTest {

    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String REFRESH_TOKEN = "test-refresh-token";

    @Autowired
    private OAuth2CookieManager cookieManager;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("Access Token과 Refresh Token 쿠키를 발급한다")
    void addTokenCookies() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        cookieManager.addTokenCookies(
                response,
                ACCESS_TOKEN,
                REFRESH_TOKEN
        );

        // then
        String accessCookie = findCookie(
                response,
                OAuth2CookieManager.ACCESS_TOKEN
        );

        String refreshCookie = findCookie(
                response,
                OAuth2CookieManager.REFRESH_TOKEN
        );

        // 각 쿠키에 올바른 토큰 값이 포함되어야 함
        assertTrue(accessCookie.startsWith(
                OAuth2CookieManager.ACCESS_TOKEN + "=" + ACCESS_TOKEN
        ));

        assertTrue(refreshCookie.startsWith(
                OAuth2CookieManager.REFRESH_TOKEN + "=" + REFRESH_TOKEN
        ));
    }

    @Test
    @DisplayName("JWT 쿠키에는 HttpOnly 속성이 설정된다")
    void cookieIsHttpOnly() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        cookieManager.addTokenCookies(
                response,
                ACCESS_TOKEN,
                REFRESH_TOKEN
        );

        // then
        assertCookieAttribute(
                response,
                OAuth2CookieManager.ACCESS_TOKEN,
                "httponly"
        );

        assertCookieAttribute(
                response,
                OAuth2CookieManager.REFRESH_TOKEN,
                "httponly"
        );
    }

    @Test
    @DisplayName("JWT 쿠키에는 SameSite=Lax가 적용된다")
    void cookieSameSiteIsLax() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        cookieManager.addTokenCookies(
                response,
                ACCESS_TOKEN,
                REFRESH_TOKEN
        );

        // then
        assertCookieAttribute(
                response,
                OAuth2CookieManager.ACCESS_TOKEN,
                "samesite=lax"
        );

        assertCookieAttribute(
                response,
                OAuth2CookieManager.REFRESH_TOKEN,
                "samesite=lax"
        );
    }

    @Test
    @DisplayName("JWT 쿠키는 루트 경로에 설정된다")
    void cookiePathIsRoot() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        cookieManager.addTokenCookies(
                response,
                ACCESS_TOKEN,
                REFRESH_TOKEN
        );

        // then
        assertCookieAttribute(
                response,
                OAuth2CookieManager.ACCESS_TOKEN,
                "path=/"
        );

        assertCookieAttribute(
                response,
                OAuth2CookieManager.REFRESH_TOKEN,
                "path=/"
        );
    }

    @Test
    @DisplayName("JWT 쿠키의 Max-Age는 설정된 만료시간과 일치한다")
    void cookieMaxAgeMatchesProperties() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        cookieManager.addTokenCookies(
                response,
                ACCESS_TOKEN,
                REFRESH_TOKEN
        );

        // then
        String accessCookie = findCookie(
                response,
                OAuth2CookieManager.ACCESS_TOKEN
        );

        String refreshCookie = findCookie(
                response,
                OAuth2CookieManager.REFRESH_TOKEN
        );

        // JwtProperties의 밀리초 설정을 초 단위로 변환
        long accessMaxAge = jwtProperties.getAccessExpiration() / 1000;
        long refreshMaxAge = jwtProperties.getRefreshExpiration() / 1000;

        assertEquals(
                accessMaxAge,
                getMaxAge(accessCookie)
        );

        assertEquals(
                refreshMaxAge,
                getMaxAge(refreshCookie)
        );
    }

    @Test
    @DisplayName("로그아웃 시 두 JWT 쿠키의 Max-Age를 0으로 설정한다")
    void clearTokenCookies() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        cookieManager.clearTokenCookies(response);

        // then
        String accessCookie = findCookie(
                response,
                OAuth2CookieManager.ACCESS_TOKEN
        );

        String refreshCookie = findCookie(
                response,
                OAuth2CookieManager.REFRESH_TOKEN
        );

        // 쿠키를 즉시 만료시키는 응답인지 확인
        assertEquals(0, getMaxAge(accessCookie));
        assertEquals(0, getMaxAge(refreshCookie));

        // 삭제 대상 쿠키의 값은 빈 문자열
        assertTrue(accessCookie.startsWith(
                OAuth2CookieManager.ACCESS_TOKEN + "="
        ));

        assertTrue(refreshCookie.startsWith(
                OAuth2CookieManager.REFRESH_TOKEN + "="
        ));
    }

    @Test
    @DisplayName("로그아웃 시 삭제 쿠키의 Path는 발급 쿠키와 일치한다")
    void clearCookieUsesSamePath() {
        // given
        MockHttpServletResponse issueResponse =
                new MockHttpServletResponse();

        MockHttpServletResponse clearResponse =
                new MockHttpServletResponse();

        // when
        cookieManager.addTokenCookies(
                issueResponse,
                ACCESS_TOKEN,
                REFRESH_TOKEN
        );

        cookieManager.clearTokenCookies(clearResponse);

        // then
        assertEquals(
                getAttribute(
                        findCookie(
                                issueResponse,
                                OAuth2CookieManager.ACCESS_TOKEN
                        ),
                        "path"
                ),
                getAttribute(
                        findCookie(
                                clearResponse,
                                OAuth2CookieManager.ACCESS_TOKEN
                        ),
                        "path"
                )
        );

        assertEquals(
                getAttribute(
                        findCookie(
                                issueResponse,
                                OAuth2CookieManager.REFRESH_TOKEN
                        ),
                        "path"
                ),
                getAttribute(
                        findCookie(
                                clearResponse,
                                OAuth2CookieManager.REFRESH_TOKEN
                        ),
                        "path"
                )
        );
    }

    // 응답의 Set-Cookie 헤더에서 지정한 이름의 쿠키 검색
    private String findCookie(
            MockHttpServletResponse response,
            String cookieName
    ) {
        List<String> cookies = response.getHeaders("Set-Cookie")
                .stream()
                .toList();

        return cookies.stream()
                .filter(cookie -> cookie.startsWith(cookieName + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "쿠키가 존재하지 않습니다: " + cookieName
                ));
    }

    // 쿠키의 특정 속성 포함 여부 검증
    private void assertCookieAttribute(
            MockHttpServletResponse response,
            String cookieName,
            String attribute
    ) {
        String cookie = findCookie(response, cookieName);

        String expected = attribute.toLowerCase(Locale.ROOT);

        boolean exists = List.of(cookie.split(";"))
                .stream()
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(expected));

        assertTrue(
                exists,
                "쿠키 속성이 없습니다: " + cookieName + ", " + attribute
        );
    }

    // Set-Cookie 헤더에서 Max-Age 값을 추출
    private long getMaxAge(String cookie) {
        String value = getAttribute(cookie, "Max-Age");

        assertNotNull(
                value,
                "Max-Age 속성이 존재하지 않습니다."
        );

        return Long.parseLong(value);
    }

    // Set-Cookie 헤더에서 지정한 속성값 추출
    private String getAttribute(
            String cookie,
            String attributeName
    ) {
        for (String part : cookie.split(";")) {
            String item = part.trim();

            String prefix = attributeName + "=";

            if (item.regionMatches(
                    true,
                    0,
                    prefix,
                    0,
                    prefix.length()
            )) {
                return item.substring(prefix.length());
            }
        }

        return null;
    }
}
