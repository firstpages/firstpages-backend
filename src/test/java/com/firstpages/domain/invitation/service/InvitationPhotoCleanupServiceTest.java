package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.dto.request.InvitationPublishRequestDto;
import com.firstpages.domain.invitation.dto.request.InvitationUpdateRequestDto;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
import com.firstpages.domain.invitation.repository.InvitationPhotoCleanupOutbox;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.firstpages.domain.invitation.entity.InvitationDocumentKind.DRAFT;
import static com.firstpages.domain.invitation.entity.InvitationDocumentKind.PUBLISHED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationPhotoCleanupServiceTest {

    private static final long OWNER_ID = 1L;
    private static final long INVITATION_ID = 42L;
    private static final int VERSION = 3;
    private static final String PREFIX = "invitations/42/photos/";
    private static final String REMOVED = PREFIX + "removed.jpg";
    private static final String SHARED = PREFIX + "shared.jpg";
    private static final String RETAINED = PREFIX + "retained.jpg";
    private static final String UPLOADED = PREFIX + "uploaded.jpg";

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationDocumentRepository invitationDocumentRepository;

    @Mock
    private InvitationPhotoService invitationPhotoService;

    @Mock
    private InvitationPhotoCleanupOutbox photoCleanupOutbox;

    @Mock
    private Invitation invitation;

    @Mock
    private InvitationDocument draft;

    @Mock
    private InvitationDocument published;

    @InjectMocks
    private InvitationServiceImpl service;

    @Test
    @DisplayName("JSON 저장은 새 작성본과 공개본에 모두 없는 기존 사진만 정리 예약한다")
    void jsonUpdateEnqueuesOnlyKeysMissingFromBothDocuments() {
        ObjectNode previousBody = bodyWithPhotos(REMOVED, SHARED, RETAINED);
        ObjectNode updatedBody = bodyWithPhotos(null, RETAINED);
        mockDraftUpdate(previousBody);
        mockDocument(PUBLISHED, published, bodyWithPhotos(SHARED));
        when(invitationPhotoService.resolvePhotos(
                INVITATION_ID, previousBody, updatedBody, Map.of()
        )).thenReturn(updatedBody);

        service.update(OWNER_ID, INVITATION_ID, updateRequest(updatedBody));

        verify(draft).updateBody(updatedBody);
        verify(photoCleanupOutbox).enqueue(Set.of(REMOVED));
    }

    @Test
    @DisplayName("첨부 사진의 저장 키를 작성본에 반영하고 공개본에 남은 사진은 보존한다")
    void multipartUpdateKeepsPublishedKeysAndUsesResolvedBody() {
        ObjectNode previousBody = bodyWithPhotos(REMOVED, SHARED);
        ObjectNode requestBody = bodyWithPhotos(null);
        requestBody.putObject("coverPhoto").put("fileRef", "cover-upload");
        ObjectNode updatedBody = bodyWithPhotos(UPLOADED);
        MultipartFile attachment = new MockMultipartFile("cover-upload", new byte[]{1});
        Map<String, List<MultipartFile>> parts = Map.of("cover-upload", List.of(attachment));
        mockDraftUpdate(previousBody);
        mockDocument(PUBLISHED, published, bodyWithPhotos(SHARED));
        when(invitationPhotoService.resolvePhotos(
                INVITATION_ID, previousBody, requestBody, parts
        )).thenReturn(updatedBody);

        service.update(OWNER_ID, INVITATION_ID, updateRequest(requestBody), parts);

        verify(draft).updateBody(updatedBody);
        verify(photoCleanupOutbox).enqueue(Set.of(REMOVED));
    }

    @Test
    @DisplayName("최초 공개는 공개본을 생성하고 사진 정리를 예약하지 않는다")
    void firstPublishCreatesDocumentWithoutEnqueuingCleanup() {
        mockLockedInvitation();
        when(invitation.getVersion()).thenReturn(VERSION);
        mockDocument(DRAFT, draft, bodyWithPhotos(SHARED));
        when(invitationDocumentRepository.findByInvitationIdAndKind(INVITATION_ID, PUBLISHED))
                .thenReturn(Optional.empty());

        service.publish(OWNER_ID, INVITATION_ID, publishRequest());

        verify(invitationDocumentRepository).save(any(InvitationDocument.class));
        verify(invitation).publish();
        verifyNoInteractions(photoCleanupOutbox);
    }

    @Test
    @DisplayName("재공개는 기존 공개본에서 빠진 사진만 정리 예약한다")
    void republishEnqueuesOnlyKeysRemovedFromPublishedBody() {
        ObjectNode draftBody = bodyWithPhotos(SHARED, RETAINED);
        mockLockedInvitation();
        when(invitation.getVersion()).thenReturn(VERSION);
        mockDocument(DRAFT, draft, draftBody);
        mockDocument(PUBLISHED, published, bodyWithPhotos(REMOVED, SHARED));

        service.publish(OWNER_ID, INVITATION_ID, publishRequest());

        verify(published).updateBody(draftBody);
        verify(photoCleanupOutbox).enqueue(Set.of(REMOVED));
    }

    @Test
    @DisplayName("공개 해제는 공개본에서만 사용하던 사진을 정리 예약한다")
    void unpublishEnqueuesOnlyPublishedKeysMissingFromDraft() {
        mockLockedInvitation();
        mockDocument(DRAFT, draft, bodyWithPhotos(SHARED));
        mockDocument(PUBLISHED, published, bodyWithPhotos(REMOVED, SHARED));

        service.unpublish(OWNER_ID, INVITATION_ID);

        verify(invitationDocumentRepository).delete(published);
        verify(invitation).unpublish();
        verify(photoCleanupOutbox).enqueue(Set.of(REMOVED));
    }

    @Test
    @DisplayName("청첩장 삭제는 두 문서에서 사용한 해당 청첩장의 사진을 중복 없이 정리 예약한다")
    void deleteEnqueuesTheUnionOfOwnedKeysWithoutDuplicates() {
        mockLockedInvitation();
        List<InvitationDocument> documents = List.of(draft, published);
        when(invitationDocumentRepository.findByInvitationId(INVITATION_ID))
                .thenReturn(documents);
        when(draft.getBody()).thenReturn(bodyWithPhotos(SHARED, RETAINED));
        when(published.getBody()).thenReturn(
                bodyWithPhotos(REMOVED, SHARED, "invitations/99/photos/other.jpg")
        );

        service.delete(OWNER_ID, INVITATION_ID);

        verify(invitationDocumentRepository).deleteAll(documents);
        verify(invitationRepository).delete(invitation);
        verify(photoCleanupOutbox).enqueue(Set.of(SHARED, RETAINED, REMOVED));
    }

    private void mockDraftUpdate(ObjectNode previousBody) {
        when(invitationRepository.findByIdAndOwnerIdWithOptimisticLock(INVITATION_ID, OWNER_ID))
                .thenReturn(Optional.of(invitation));
        when(invitation.getVersion()).thenReturn(VERSION);
        mockDocument(DRAFT, draft, previousBody);
    }

    private void mockLockedInvitation() {
        when(invitationRepository.findByOwnerIdAndIdForUpdate(OWNER_ID, INVITATION_ID))
                .thenReturn(Optional.of(invitation));
    }

    private void mockDocument(
            InvitationDocumentKind kind,
            InvitationDocument document,
            ObjectNode body
    ) {
        when(invitationDocumentRepository.findByInvitationIdAndKind(INVITATION_ID, kind))
                .thenReturn(Optional.of(document));
        when(document.getBody()).thenReturn(body);
    }

    private ObjectNode bodyWithPhotos(String coverKey, String... galleryKeys) {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        if (coverKey == null) {
            body.putNull("coverPhoto");
        } else {
            body.putObject("coverPhoto").put("storageKey", coverKey);
        }

        var gallery = body.putObject("gallery").putArray("photos");
        for (String galleryKey : galleryKeys) {
            gallery.addObject().put("storageKey", galleryKey);
        }
        return body;
    }

    private InvitationUpdateRequestDto updateRequest(ObjectNode body) {
        InvitationUpdateRequestDto request = BeanUtils.instantiateClass(InvitationUpdateRequestDto.class);
        ReflectionTestUtils.setField(request, "version", VERSION);
        ReflectionTestUtils.setField(request, "body", body);
        ReflectionTestUtils.setField(request, "managementTitle", "청첩장");
        return request;
    }

    private InvitationPublishRequestDto publishRequest() {
        InvitationPublishRequestDto request = BeanUtils.instantiateClass(InvitationPublishRequestDto.class);
        ReflectionTestUtils.setField(request, "version", VERSION);
        return request;
    }
}
