package com.firstpages.domain.user.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 회원 탈퇴 트랜잭션과 커밋 후 후속 처리를 연결하는 이벤트
@Getter
@RequiredArgsConstructor
public final class AccountWithdrawn {

    // Redis 토큰 정리에 사용하는 내부 회원 ID
    private final Long userId;
}
