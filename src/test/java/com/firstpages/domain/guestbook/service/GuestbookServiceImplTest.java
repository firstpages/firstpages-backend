package com.firstpages.domain.guestbook.service;

import com.firstpages.domain.guestbook.dto.request.GuestbookCreateRequestDto;
import com.firstpages.domain.guestbook.dto.response.GuestbookItemResponseDto;
import com.firstpages.domain.guestbook.entity.Guestbook;
import com.firstpages.domain.guestbook.exception.GuestbookErrorCode;
import com.firstpages.domain.guestbook.repository.GuestbookRepository;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.global.exception.BusinessException;
import com.firstpages.global.response.code.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// 공개 상태와 방명록 설정에 따른 메시지 저장을 검증한다.
@ExtendWith(MockitoExtension.class)
@DisplayName("방명록 서비스")
class GuestbookServiceImplTest {

    // 테스트 청첩장의 내부 식별자
    private static final Long INVITATION_ID = 42L;

    // 하객이 사용하는 공유 식별자
    private static final String PUBLIC_ID = "0123456789abcdef0123456789abcdef";

    // 청첩장 잠금 조회 결과를 제어한다.
    @Mock
    private InvitationRepository invitationRepository;

    // 현재 공개본 조회 결과를 제어한다.
    @Mock
    private InvitationDocumentRepository invitationDocumentRepository;

    // 저장 요청에 담긴 방명록 메시지를 확인한다.
    @Mock
    private GuestbookRepository guestbookRepository;

    // 방명록 작성 조건을 검증하는 실제 서비스
    @InjectMocks
    private GuestbookServiceImpl guestbookService;

    // 잠금 조회한 청첩장에 입력한 이름과 메시지가 저장되는지 확인한다.
    @Test
    @DisplayName("공개본에서 방명록이 켜져 있으면 청첩장에 이름과 메시지를 저장한다")
    void savesMessageForPublishedInvitation() {
        Invitation invitation = createInvitation(true);
        stubPublishedDocument(invitation, "true");

        guestbookService.create(PUBLIC_ID, createRequest());

        ArgumentCaptor<Guestbook> saved = ArgumentCaptor.forClass(Guestbook.class);
        verify(guestbookRepository).save(saved.capture());
        assertThat(saved.getValue().getInvitation()).isSameAs(invitation);
        assertThat(saved.getValue().getAuthorName()).isEqualTo("김하객");
        assertThat(saved.getValue().getMessage()).isEqualTo("결혼 축하해요!");
    }

    // 공유 ID가 없으면 공개본 조회와 메시지 저장을 하지 않는다.
    @Test
    @DisplayName("공유 ID에 해당하는 청첩장이 없으면 메시지를 저장하지 않는다")
    void rejectsUnknownInvitation() {
        when(invitationRepository.findByPublicIdForUpdate(PUBLIC_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestbookService.create(PUBLIC_ID, createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND));
        verifyNoInteractions(invitationDocumentRepository, guestbookRepository);
    }

    // 현재 청첩장이 비공개면 공개본 조회와 메시지 저장을 하지 않는다.
    @Test
    @DisplayName("비공개 청첩장은 공개본을 조회하지 않고 메시지 작성을 거절한다")
    void rejectsPrivateInvitation() {
        when(invitationRepository.findByPublicIdForUpdate(PUBLIC_ID))
                .thenReturn(Optional.of(createInvitation(false)));

        assertThatThrownBy(() -> guestbookService.create(PUBLIC_ID, createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND));
        verifyNoInteractions(invitationDocumentRepository, guestbookRepository);
    }

    // 부모 상태가 공개여도 현재 공개본이 있어야 작성할 수 있다.
    @Test
    @DisplayName("청첩장이 공개 상태여도 공개본이 없으면 메시지 작성을 거절한다")
    void rejectsMissingPublishedDocument() {
        when(invitationRepository.findByPublicIdForUpdate(PUBLIC_ID))
                .thenReturn(Optional.of(createInvitation(true)));
        when(invitationDocumentRepository.findByInvitationIdAndKind(
                INVITATION_ID, InvitationDocumentKind.PUBLISHED
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestbookService.create(PUBLIC_ID, createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND));
        verifyNoInteractions(guestbookRepository);
    }

    // 방명록이 꺼져 있거나 설정 값이 올바른 불리언이 아니면 저장하지 않는다.
    @ParameterizedTest(name = "{displayName} (설정: {0})")
    @ValueSource(strings = {"false", "문자열 true", "누락"})
    @DisplayName("공개본의 방명록 설정이 불리언 true가 아니면 메시지 작성을 거절한다")
    void rejectsDisabledGuestbook(String setting) {
        stubPublishedDocument(createInvitation(true), setting);

        assertThatThrownBy(() -> guestbookService.create(PUBLIC_ID, createRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(GuestbookErrorCode.GUESTBOOK_DISABLED));
        verifyNoInteractions(guestbookRepository);
    }

    // 공개본 없이도 소유자가 다음 페이지를 조회할 수 있는지 확인한다.
    @Test
    @DisplayName("소유자는 비공개 청첩장의 방명록도 다음 페이지로 조회할 수 있다")
    void listsPrivateInvitationGuestbooks() {
        Long ownerId = 7L;
        PageRequest request = PageRequest.of(1, 20);
        Guestbook entry = Guestbook.create(createInvitation(false), "김하객", "축하합니다.");
        LocalDateTime writtenAt = LocalDateTime.of(2026, 11, 26, 12, 0);
        ReflectionTestUtils.setField(entry, "id", 21L);
        ReflectionTestUtils.setField(entry, "createdAt", writtenAt);

        when(invitationRepository.existsByIdAndOwnerId(INVITATION_ID, ownerId))
                .thenReturn(true);
        when(guestbookRepository.findByInvitationIdOrderByCreatedAtDescIdDesc(
                INVITATION_ID, request
        )).thenReturn(new PageImpl<>(List.of(entry), request, 21));

        Page<GuestbookItemResponseDto> page =
                guestbookService.getGuestbooks(ownerId, INVITATION_ID, 1, 20);

        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(21L);
            assertThat(item.getAuthorName()).isEqualTo("김하객");
            assertThat(item.getMessage()).isEqualTo("축하합니다.");
            assertThat(item.getCreatedAt()).isEqualTo(writtenAt);
        });
        verifyNoInteractions(invitationDocumentRepository);
    }

    // 소유권이 확인되지 않으면 메시지 조회를 차단한다.
    @Test
    @DisplayName("다른 사용자의 청첩장은 방명록 메시지를 조회할 수 없다")
    void rejectsNonOwnerListing() {
        Long requesterId = 8L;
        when(invitationRepository.existsByIdAndOwnerId(INVITATION_ID, requesterId))
                .thenReturn(false);

        assertThatThrownBy(() -> guestbookService.getGuestbooks(requesterId, INVITATION_ID, 0, 20))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND));
        verifyNoInteractions(guestbookRepository);
    }

    // 페이지 범위를 벗어난 요청은 데이터 조회 전에 거절한다.
    @ParameterizedTest(name = "{displayName} (페이지 {0}, 크기 {1})")
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    @DisplayName("페이지 번호나 크기가 허용 범위를 벗어나면 목록 조회를 거절한다")
    void rejectsInvalidPage(int page, int size) {
        assertThatThrownBy(() -> guestbookService.getGuestbooks(7L, INVITATION_ID, page, size))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(CommonErrorCode.INVALID_PARAMETER));
        verifyNoInteractions(invitationRepository, guestbookRepository);
    }

    // 소유자의 요청으로 청첩장과 메시지 ID가 일치하는 행을 삭제한다.
    @Test
    @DisplayName("소유자는 자신의 청첩장에 달린 메시지 한 건을 삭제할 수 있다")
    void deletesOwnedGuestbook() {
        Long ownerId = 7L;
        Long guestbookId = 21L;
        when(invitationRepository.existsByIdAndOwnerId(INVITATION_ID, ownerId))
                .thenReturn(true);
        when(guestbookRepository.deleteByInvitationIdAndGuestbookId(INVITATION_ID, guestbookId))
                .thenReturn(1);

        guestbookService.delete(ownerId, INVITATION_ID, guestbookId);

        verify(guestbookRepository).deleteByInvitationIdAndGuestbookId(INVITATION_ID, guestbookId);
        verifyNoInteractions(invitationDocumentRepository);
    }

    // 청첩장 소유권이 없으면 방명록 삭제 쿼리를 호출하지 않는다.
    @Test
    @DisplayName("청첩장 소유자가 아니면 메시지를 삭제하지 않는다")
    void rejectsNonOwnerDeletion() {
        Long requesterId = 8L;
        when(invitationRepository.existsByIdAndOwnerId(INVITATION_ID, requesterId))
                .thenReturn(false);

        assertThatThrownBy(() -> guestbookService.delete(requesterId, INVITATION_ID, 21L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND));
        verifyNoInteractions(guestbookRepository);
    }

    // 삭제 건수가 0이면 요청한 청첩장에 해당 메시지가 없는 것으로 처리한다.
    @Test
    @DisplayName("삭제 조건과 일치하는 메시지가 없으면 GUESTBOOK_NOT_FOUND를 반환한다")
    void rejectsDeletionWhenMessageDoesNotBelongToInvitation() {
        Long ownerId = 7L;
        Long guestbookId = 21L;
        when(invitationRepository.existsByIdAndOwnerId(INVITATION_ID, ownerId))
                .thenReturn(true);
        when(guestbookRepository.deleteByInvitationIdAndGuestbookId(INVITATION_ID, guestbookId))
                .thenReturn(0);

        assertThatThrownBy(() -> guestbookService.delete(ownerId, INVITATION_ID, guestbookId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(GuestbookErrorCode.GUESTBOOK_NOT_FOUND));
    }

    // 원하는 공개 상태를 가진 청첩장을 만든다.
    private Invitation createInvitation(boolean published) {
        Invitation invitation = Invitation.create(null, "테스트 청첩장", PUBLIC_ID);
        ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
        if (published) {
            invitation.publish();
        }
        return invitation;
    }

    // 방명록 설정을 담은 공개본을 잠금 조회한 청첩장과 연결한다.
    private void stubPublishedDocument(Invitation invitation, String setting) {
        var body = JsonNodeFactory.instance.objectNode();
        var guestbook = body.putObject("guestbook");
        switch (setting) {
            case "true" -> guestbook.put("enabled", true);
            case "false" -> guestbook.put("enabled", false);
            case "문자열 true" -> guestbook.put("enabled", "true");
            case "누락" -> { }
            default -> throw new IllegalArgumentException(setting);
        }
        InvitationDocument published = InvitationDocument.create(
                invitation, InvitationDocumentKind.PUBLISHED, null, body
        );

        when(invitationRepository.findByPublicIdForUpdate(PUBLIC_ID))
                .thenReturn(Optional.of(invitation));
        when(invitationDocumentRepository.findByInvitationIdAndKind(
                INVITATION_ID, InvitationDocumentKind.PUBLISHED
        )).thenReturn(Optional.of(published));
    }

    // 이름과 메시지가 담긴 서비스 요청을 만든다.
    private GuestbookCreateRequestDto createRequest() {
        GuestbookCreateRequestDto request = BeanUtils.instantiateClass(
                GuestbookCreateRequestDto.class
        );
        ReflectionTestUtils.setField(request, "authorName", "김하객");
        ReflectionTestUtils.setField(request, "message", "결혼 축하해요!");
        return request;
    }
}
