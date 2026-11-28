package com.firstpages.domain.auth.event;

import com.firstpages.domain.auth.service.RefreshTokenService;
import com.firstpages.domain.user.event.AccountWithdrawn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// 탈퇴 이벤트와 Redis 정리 시점 검증
class WithdrawnUserRefreshTokenListenerTest {

    private static final Long USER_ID = 9L;

    @Test
    @DisplayName("탈퇴 이벤트의 Redis 정리는 커밋 이후에 실행된다")
    void deletesOnlyAfterCommit() {
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);

        try (AnnotationConfigApplicationContext context = listenerContext(refreshTokenService)) {
            transaction().executeWithoutResult(status -> {
                context.publishEvent(new AccountWithdrawn(USER_ID));
                verifyNoInteractions(refreshTokenService);
            });

            verify(refreshTokenService).delete(USER_ID);
        }
    }

    @Test
    @DisplayName("탈퇴 트랜잭션이 롤백되면 Redis를 정리하지 않는다")
    void skipsDeletionAfterRollback() {
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);

        try (AnnotationConfigApplicationContext context = listenerContext(refreshTokenService)) {
            transaction().executeWithoutResult(status -> {
                context.publishEvent(new AccountWithdrawn(USER_ID));
                status.setRollbackOnly();
            });

            verifyNoInteractions(refreshTokenService);
        }
    }

    @Test
    @DisplayName("커밋 후 Redis 장애가 호출자에게 전파되지 않는다")
    void redisFailureDoesNotPropagateAfterCommit() {
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        doThrow(new DataAccessResourceFailureException("Redis unavailable"))
                .when(refreshTokenService).delete(USER_ID);

        try (AnnotationConfigApplicationContext context = listenerContext(refreshTokenService)) {
            assertDoesNotThrow(() -> transaction().executeWithoutResult(
                    status -> context.publishEvent(new AccountWithdrawn(USER_ID))
            ));

            verify(refreshTokenService).delete(USER_ID);
        }
    }

    // 실제 Spring 이벤트 리스너와 대체 Redis 서비스를 등록
    private AnnotationConfigApplicationContext listenerContext(RefreshTokenService refreshTokenService) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(RefreshTokenService.class, () -> refreshTokenService);
        context.register(TransactionalEventListenerFactory.class, WithdrawnUserRefreshTokenListener.class);
        context.refresh();
        return context;
    }

    // 외부 DB 없이 Spring 트랜잭션 동기화의 커밋과 롤백을 실행
    private TransactionTemplate transaction() {
        return new TransactionTemplate(new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        });
    }
}
