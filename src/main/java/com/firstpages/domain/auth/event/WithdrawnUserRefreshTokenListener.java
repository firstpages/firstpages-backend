package com.firstpages.domain.auth.event;

import com.firstpages.domain.auth.service.RefreshTokenService;
import com.firstpages.domain.user.event.AccountWithdrawn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WithdrawnUserRefreshTokenListener {

    // 회원별 Refresh Token 삭제에 사용하는 서비스
    private final RefreshTokenService refreshTokenService;

    // 커밋에 성공한 회원의 Redis 토큰 삭제 시도
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountWithdrawn(AccountWithdrawn event) {

        // 이벤트에서 삭제된 회원의 내부 ID 추출
        Long userId = event.getUserId();

        // 회원 ID에 해당하는 Redis Refresh Token 삭제
        refreshTokenService.delete(userId);
    }
}
