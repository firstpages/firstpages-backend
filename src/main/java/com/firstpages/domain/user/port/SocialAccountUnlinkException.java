package com.firstpages.domain.user.port;

// 외부 계정의 연결 해제를 확인할 수 없을 때 발생하는 예외
public class SocialAccountUnlinkException extends RuntimeException {

    // 실패 사유를 담은 예외 생성
    public SocialAccountUnlinkException(String message) {
        super(message);
    }

    // 실패 사유와 원인을 담은 예외 생성
    public SocialAccountUnlinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
