package com.firstpages.infrastructure.kakao;

import com.firstpages.domain.user.entity.SocialProvider;
import com.firstpages.domain.user.port.SocialAccountUnlinkException;
import com.firstpages.domain.user.port.SocialAccountUnlinker;
import com.firstpages.infrastructure.kakao.dto.KakaoErrorResponseDto;
import com.firstpages.infrastructure.kakao.dto.UnlinkResponseDto;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// 카카오 API로 외부 계정의 연결 해제 계약을 구현하는 클라이언트
@Component
public class KakaoUnlinkClient implements SocialAccountUnlinker {

    private static final int KAKAO_NOT_LINKED = -101;

    // 연결 해제 API에 사용하는 HTTP 클라이언트
    private final RestClient client;

    public KakaoUnlinkClient(
            @Value("${app.kakao.service-app-admin-key}") String adminKey
    ) {
        // 키가 비어 있으면 시작 단계에서 설정 오류
        if (adminKey == null || adminKey.isBlank()) {
            throw new IllegalArgumentException("KAKAO_SERVICE_APP_ADMIN_KEY is required");
        }

        // 연결 및 읽기 타임아웃 설정
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(3_000);

        // 카카오 API 주소와 서버용 인증 헤더 등록
        this.client = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                .build();
    }

    // 카카오 회원번호로 연결을 해제하고 응답 대상 검증
    @Override
    public void unlink(
            SocialProvider provider,
            String providerId
    ) {

        // 카카오 이외의 제공자 요청 차단
        if (provider != SocialProvider.KAKAO) {
            throw new SocialAccountUnlinkException("Unsupported social provider");
        }

        // 카카오 회원번호를 연결 해제 대상에 지정
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", providerId);

        // 성공 응답의 회원번호를 읽기 위한 변수
        UnlinkResponseDto response;

        try {
            // 폼 데이터로 카카오 연결 해제 요청 전송
            response = client.post()
                    .uri("/v1/user/unlink")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(UnlinkResponseDto.class);
        } catch (RestClientResponseException exception) {
            // 이미 연결 해제된 경우 정상 완료
            if (isAlreadyUnlinked(exception)) {
                return;
            }

            // 카카오의 나머지 HTTP 오류를 포트 예외로 변환
            throw new SocialAccountUnlinkException(
                    "Kakao unlink rejected with HTTP " + exception.getStatusCode().value(),
                    exception
            );
        } catch (RestClientException exception) {
            // 타임아웃과 전송 오류를 포트 예외로 변환
            throw new SocialAccountUnlinkException(
                    "Kakao unlink request failed",
                    exception
            );
        }

        // 해제 응답의 회원번호가 요청 대상과 일치하는지 확인
        if (response == null
                || response.getId() == null
                || !providerId.equals(response.getId().toString())
        ) {
            throw new SocialAccountUnlinkException("Unexpected Kakao unlink response");
        }
    }

    // HTTP 400 응답에서 카카오 미연결 코드 확인
    private boolean isAlreadyUnlinked(RestClientResponseException exception) {
        // 미연결 응답 검사 대상을 HTTP 400으로 제한
        if (exception.getStatusCode().value() != 400) {
            return false;
        }

        try {
            // 카카오 오류 본문에서 미연결 코드 추출
            KakaoErrorResponseDto error = exception.getResponseBodyAs(KakaoErrorResponseDto.class);
            return error != null && Integer.valueOf(KAKAO_NOT_LINKED).equals(error.getCode());
        } catch (RestClientException | IllegalStateException ignored) {
            // 오류 본문을 확인할 수 없으면 해제 실패로 처리
            return false;
        }
    }
}
