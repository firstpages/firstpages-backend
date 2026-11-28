package com.firstpages.domain.user.port;

import com.firstpages.domain.user.entity.SocialProvider;

// 회원 서비스가 외부 계정의 연결 해제를 요청하는 계약
public interface SocialAccountUnlinker {

    // 연결 해제를 확인하고 결과가 불확실하면 예외 발생
    void unlink(
            SocialProvider provider,
            String providerId
    );
}
