package com.firstpages.domain.invitation.scheduler;

import com.firstpages.domain.invitation.repository.InvitationPhotoCleanupOutbox;
import com.firstpages.domain.invitation.service.InvitationPhotoStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// S3 삭제 결과에 따라 아웃박스를 완료하거나 재시도하는지 검증한다.
@ExtendWith(MockitoExtension.class)
class InvitationPhotoCleanupJobTest {

    private static final String FIRST_KEY = "invitations/187/photos/first.jpeg";
    private static final String SECOND_KEY = "invitations/187/photos/second.jpeg";

    @Mock
    private InvitationPhotoCleanupOutbox outbox;

    @Mock
    private InvitationPhotoStorage photoStorage;

    @InjectMocks
    private InvitationPhotoCleanupJob cleanupJob;

    // 사진을 먼저 삭제한 뒤 아웃박스 요청을 완료한다.
    @Test
    @DisplayName("S3 삭제가 성공하면 해당 아웃박스 요청을 완료한다")
    void completesRequestAfterPhotoDeletion() {
        when(outbox.findDueKeys()).thenReturn(List.of(FIRST_KEY));

        cleanupJob.cleanUp();

        var order = inOrder(photoStorage, outbox);
        order.verify(photoStorage).delete(FIRST_KEY);
        order.verify(outbox).complete(FIRST_KEY);
        verify(outbox, never()).reschedule(FIRST_KEY);
    }

    // 실패한 요청을 재예약하고 같은 배치의 다음 사진을 처리한다.
    @Test
    @DisplayName("S3 삭제에 실패하면 재시도를 예약하고 다음 사진을 처리한다")
    void reschedulesFailedRequestAndProcessesNextPhoto() {
        when(outbox.findDueKeys()).thenReturn(List.of(FIRST_KEY, SECOND_KEY));
        doThrow(new IllegalStateException("S3 삭제 실패"))
                .when(photoStorage).delete(FIRST_KEY);

        cleanupJob.cleanUp();

        var order = inOrder(photoStorage, outbox);
        order.verify(photoStorage).delete(FIRST_KEY);
        order.verify(outbox).reschedule(FIRST_KEY);
        order.verify(photoStorage).delete(SECOND_KEY);
        order.verify(outbox).complete(SECOND_KEY);
        verify(outbox, never()).complete(FIRST_KEY);
    }

    // 처리할 요청이 없으면 S3를 호출하지 않는다.
    @Test
    @DisplayName("실행할 정리 요청이 없으면 S3에 접근하지 않는다")
    void skipsStorageWhenNoRequestIsDue() {
        when(outbox.findDueKeys()).thenReturn(List.of());

        cleanupJob.cleanUp();

        verifyNoInteractions(photoStorage);
    }
}