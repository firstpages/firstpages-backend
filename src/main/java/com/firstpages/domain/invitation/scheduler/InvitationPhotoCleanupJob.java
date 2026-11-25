package com.firstpages.domain.invitation.scheduler;

import com.firstpages.domain.invitation.repository.InvitationPhotoCleanupOutbox;
import com.firstpages.domain.invitation.service.InvitationPhotoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 커밋된 사진 정리 요청을 처리
@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationPhotoCleanupJob {

    private final InvitationPhotoCleanupOutbox outbox;

    private final InvitationPhotoStorage photoStorage;

    // 정리할 사진을 삭제하고 실패한 요청은 재시도 대상으로 설정
    @Scheduled(fixedDelayString = "${app.invitation-photos.cleanup-delay-ms:60000}")
    public void cleanUp() {
        // 만료된 스토리지 키를 조회
        for (String storageKey : outbox.findDueKeys()) {
            try {
                // S3에서 삭제
                photoStorage.delete(storageKey);
                // DB에서 완료 처리
                outbox.complete(storageKey);
            } catch (RuntimeException e) {
                log.warn("청첩장 사진 정리 실패. storageKey={}", storageKey, e);
                // DB에 재등록
                outbox.reschedule(storageKey);
            }
        }
    }
}
