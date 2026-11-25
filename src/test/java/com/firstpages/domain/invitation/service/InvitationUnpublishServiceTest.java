package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.entity.InvitationStatus;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// 청첩장 서비스의 공개 해제 동작을 검증한다.
@ExtendWith(MockitoExtension.class)
class InvitationUnpublishServiceTest {

    private static final Long OWNER_ID = 9L;
    private static final Long OTHER_USER_ID = 10L;
    private static final Long INVITATION_ID = 187L;
    private static final LocalDateTime PUBLISHED_AT =
            LocalDateTime.of(2026, 11, 24, 15, 0);

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationDocumentRepository invitationDocumentRepository;

    @Mock
    private InvitationDocument publishedDocument;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    // 공개본 삭제를 요청하고 마지막 공개 시각을 보존하는지 확인한다.
    @Test
    @DisplayName("소유자가 공개를 해제하면 공개본을 삭제하고 작성 중 상태로 전환한다")
    void unpublishDeletesPublishedDocumentAndChangesStatus() {
        Invitation invitation = createInvitation(InvitationStatus.PUBLISHED);

        when(invitationRepository.findByOwnerIdAndIdForUpdate(OWNER_ID, INVITATION_ID))
                .thenReturn(Optional.of(invitation));
        when(invitationDocumentRepository.findByInvitationIdAndKind(
                INVITATION_ID,
                InvitationDocumentKind.PUBLISHED
        )).thenReturn(Optional.of(publishedDocument));

        when(publishedDocument.getBody()).thenReturn(JsonNodeFactory.instance.objectNode());

        invitationService.unpublish(OWNER_ID, INVITATION_ID);

        verify(invitationDocumentRepository).findByInvitationIdAndKind(
                INVITATION_ID,
                InvitationDocumentKind.PUBLISHED
        );
        verify(invitationDocumentRepository).delete(publishedDocument);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.DRAFT);
        assertThat(invitation.getPublishedAt()).isEqualTo(PUBLISHED_AT);
    }

    // 공개본이 없는 청첩장에 대한 반복 요청도 성공하는지 확인한다.
    @Test
    @DisplayName("이미 비공개인 청첩장은 반복 요청에도 성공한다")
    void unpublishSucceedsWhenAlreadyDraft() {
        Invitation invitation = createInvitation(InvitationStatus.DRAFT);

        when(invitationRepository.findByOwnerIdAndIdForUpdate(OWNER_ID, INVITATION_ID))
                .thenReturn(Optional.of(invitation));
        when(invitationDocumentRepository.findByInvitationIdAndKind(
                INVITATION_ID,
                InvitationDocumentKind.PUBLISHED
        )).thenReturn(Optional.empty());

        assertThatCode(() -> invitationService.unpublish(OWNER_ID, INVITATION_ID))
                .doesNotThrowAnyException();

        // 공개본 조회만 허용하고 추가 삭제가 없는지 검증한다.
        verify(invitationDocumentRepository, org.mockito.Mockito.only())
                .findByInvitationIdAndKind(INVITATION_ID, InvitationDocumentKind.PUBLISHED);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.DRAFT);
        assertThat(invitation.getPublishedAt()).isEqualTo(PUBLISHED_AT);
    }

    // 소유권을 확인할 수 없으면 문서에 접근하기 전에 요청을 거절한다.
    @Test
    @DisplayName("다른 사용자의 청첩장은 공개를 해제할 수 없다")
    void unpublishRejectsNonOwner() {
        when(invitationRepository.findByOwnerIdAndIdForUpdate(OTHER_USER_ID, INVITATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.unpublish(OTHER_USER_ID, INVITATION_ID))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(invitationDocumentRepository);
    }

    // 공개 이력이 있는 청첩장 엔티티를 테스트용으로 구성한다.
    private Invitation createInvitation(InvitationStatus status) {
        Invitation invitation = BeanUtils.instantiateClass(Invitation.class);
        ReflectionTestUtils.setField(invitation, "status", status);
        ReflectionTestUtils.setField(invitation, "publishedAt", PUBLISHED_AT);
        return invitation;
    }
}