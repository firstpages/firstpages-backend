package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.dto.request.InvitationCreateRequestDto;
import com.firstpages.domain.invitation.dto.request.InvitationPublishRequestDto;
import com.firstpages.domain.invitation.dto.request.InvitationUpdateRequestDto;
import com.firstpages.domain.invitation.dto.response.*;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.mapper.InvitationOwnerPhotoMapper;
import com.firstpages.domain.invitation.mapper.InvitationPublicBodyMapper;
import com.firstpages.domain.invitation.mapper.InvitationPublicPhotoUrlResolver;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
import com.firstpages.domain.invitation.repository.InvitationPhotoCleanupOutbox;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.template.exception.TemplateErrorCode;
import com.firstpages.domain.template.repository.TemplateRepository;
import com.firstpages.domain.user.entity.User;
import com.firstpages.domain.user.exception.UserErrorCode;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

// 청첩장 생성과 관리용 조회의 비즈니스 로직을 처리
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationServiceImpl implements InvitationService {

    // 사용자 Repository
    private final UserRepository userRepository;

    // 청첩장 Repository
    private final InvitationRepository invitationRepository;

    // 청첩장 문서 Repository
    private final InvitationDocumentRepository invitationDocumentRepository;

    // 템플릿 Repository
    private final TemplateRepository templateRepository;

    // 초기 청첩장 작성본 JSON 생성 컴포넌트
    private final InvitationDraftBodyFactory draftBodyFactory;

    // 저장된 공개본을 하객용 JSON으로 변환
    private final InvitationPublicBodyMapper publicBodyMapper;

    // 기존 사진 참조를 검증하고 첨부 사진을 저장
    private final InvitationPhotoService invitationPhotoService;

    // 소유자용 사진 조회 URL을 응답에 추가
    private final InvitationOwnerPhotoMapper ownerPhotoMapper;

    // 공개본 사진의 CloudFront 조회 URL을 생성
    private final InvitationPublicPhotoUrlResolver publicPhotoUrlResolver;

    // 청첩장 삭제와 함께 사진 정리 요청을 기록
    private final InvitationPhotoCleanupOutbox photoCleanupOutbox;

    // 선택 가능한 템플릿으로 청첩장을 생성하고 초기 작성본을 함께 저장
    @Transactional
    @Override
    public InvitationCreateResponseDto create(
            Long userId,
            InvitationCreateRequestDto requestDto
    ) {
        // 로그인한 사용자 조회
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        UserErrorCode.USER_NOT_FOUND
                ));

        // 선택한 템플릿 조회
        Template template = templateRepository.findById(requestDto.getTemplateId())
                .orElseThrow(() -> new BusinessException(
                        TemplateErrorCode.TEMPLATE_NOT_FOUND
                ));

        // 템플릿 선택 가능 여부 검증
        if (!template.isSelectable()) {
            throw new BusinessException(
                    TemplateErrorCode.TEMPLATE_NOT_SELECTABLE
            );
        }

        // 청첩장 공유 식별자 생성
        String publicId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        // 기본 청첩장 엔티티 생성
        Invitation invitation = Invitation.create(
                owner,
                "",
                publicId
        );

        // 청첩장 저장
        Invitation savedInvitation = invitationRepository.save(invitation);

        // 청첩장 작성본 엔티티 생성
        InvitationDocument document = InvitationDocument.create(
                savedInvitation,
                InvitationDocumentKind.DRAFT,
                template,
                draftBodyFactory.create(template)
        );

        // 청첩장 작성본 저장
        invitationDocumentRepository.save(document);

        // 생성된 청첩장 정보 반환
        return InvitationCreateResponseDto.from(savedInvitation);
    }

    // 사용자의 모든 청첩장 작성본을 조회
    @Override
    public List<InvitationListResponseDto> getInvitations(Long userId) {
        return invitationDocumentRepository.findAllDraftsByOwnerId(userId)
                .stream()
                .map(document -> InvitationListResponseDto.of(
                                document,
                                ownerPhotoMapper.toCoverPhoto(document.getBody())
                        )
                )
                .toList();
    }

    @Override
    public InvitationDetailResponseDto getDetail(
            Long userId,
            Long invitationId
    ) {
        InvitationDocument draft = invitationDocumentRepository.findDraftByInvitationIdAndOwnerId(invitationId, userId)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 작성본의 복사본에 편집 화면에서 사용할 사진 URL을 추가
        JsonNode responseBody = ownerPhotoMapper.toDetailBody(draft.getBody());

        return InvitationDetailResponseDto.of(
                draft,
                responseBody
        );
    }

    // 새 파일 첨부 없이 작성본을 저장
    @Transactional
    @Override
    public void update(
            Long userId,
            Long invitationId,
            InvitationUpdateRequestDto requestDto
    ) {
        // 빈 첨부 맵을 넘겨 기존 사진 참조 검증과 삭제 예약을 같은 흐름에서 처리
        update(
                userId,
                invitationId,
                requestDto,
                Map.of()
        );
    }

    // 첨부 사진을 포함한 작성본을 저장
    @Transactional
    @Override
    public void update(
            Long userId,
            Long invitationId,
            InvitationUpdateRequestDto requestDto,
            Map<String, List<MultipartFile>> photoParts
    ) {
        // 작성본은 JSON 객체만 허용
        if (!(requestDto.getBody() instanceof ObjectNode requestBody)) {
            throw new BusinessException(
                    InvitationErrorCode.INVITATION_INVALID_BODY
            );
        }

        // 소유권을 확인하고 본문만 바뀌는 저장에도 부모 버전이 증가
        Invitation invitation = invitationRepository
                .findByIdAndOwnerIdWithOptimisticLock(invitationId, userId)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 오래된 요청은 사진 업로드 전에 거절
        if (!Objects.equals(requestDto.getVersion(), invitation.getVersion())) {
            throw new BusinessException(
                    InvitationErrorCode.INVITATION_VERSION_CONFLICT
            );
        }

        // 저장 전 작성본을 조회해 기존 사진 참조 검증과 삭제 대상 비교에 사용
        InvitationDocument document = invitationDocumentRepository
                .findByInvitationIdAndKind(invitationId, InvitationDocumentKind.DRAFT)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_DRAFT_NOT_FOUND
                ));

        // 저장 전 작성본의 Body
        JsonNode previousBody = document.getBody();

        // 기존 사진 참조를 검증하고, 새 첨부가 있으면 업로드 후 fileRef를 storageKey로 변경
        ObjectNode updatedBody = invitationPhotoService.resolvePhotos(
                invitationId,
                previousBody,
                requestBody,
                photoParts
        );

        // 이전 작성본의 키에서 실제로 저장할 새 작성본에 남은 키를 제거
        Set<String> removedPhotoKeys = collectPhotoKeys(invitationId, previousBody);

        removedPhotoKeys.removeAll(collectPhotoKeys(invitationId, updatedBody));

        // 빠진 사진이 있을 경우 공개본을 조회한 후 여전히 사용 중(공개본)인 키를 제외
        if (!removedPhotoKeys.isEmpty()) {
            invitationDocumentRepository
                    .findByInvitationIdAndKind(invitationId, InvitationDocumentKind.PUBLISHED)
                    .ifPresent(published -> removedPhotoKeys.removeAll(
                            collectPhotoKeys(invitationId, published.getBody())
                    ));
        }

        // 관리용 제목과 사진 키가 확정된 작성본을 갱신
        invitation.updateManagementTitle(requestDto.getManagementTitle());

        document.updateBody(updatedBody);

        // 최종 작성본과 공개본에 모두 없는 기존 사진을 같은 트랜잭션의 Outbox에 예약
        if (!removedPhotoKeys.isEmpty()) {
            photoCleanupOutbox.enqueue(removedPhotoKeys);
        }
    }

    // 저장한 청첩장의 ID와 현재 버전을 조회
    @Override
    public InvitationUpdateResponseDto getUpdateResult(
            Long userId,
            Long invitationId
    ) {
        Invitation invitation = invitationRepository.findByOwnerIdAndId(
                userId,
                invitationId
        ).orElseThrow(() -> new BusinessException(
                InvitationErrorCode.INVITATION_NOT_FOUND
        ));

        return InvitationUpdateResponseDto.from(invitation);
    }

    // 작성본을 공개하고, 재공개로 빠진 사진의 정리를 예약
    @Transactional
    @Override
    public void publish(
            Long userId,
            Long invitationId,
            InvitationPublishRequestDto requestDto
    ) {
        // 소유권을 확인하고 공개 처리가 끝날 때까지 부모 행을 잠금
        Invitation invitation = invitationRepository
                .findByOwnerIdAndIdForUpdate(userId, invitationId)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 잠금을 획득한 뒤 요청 버전과 현재 버전을 비교
        if (!Objects.equals(requestDto.getVersion(), invitation.getVersion())) {
            throw new BusinessException(
                    InvitationErrorCode.INVITATION_VERSION_CONFLICT
            );
        }

        // 이번에 공개할 작성본을 조회
        InvitationDocument draft = invitationDocumentRepository
                .findByInvitationIdAndKind(invitationId, InvitationDocumentKind.DRAFT)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_DRAFT_NOT_FOUND
                ));

        // 이번에 공개할 작성본의 Body
        JsonNode draftBody = draft.getBody();

        // 공개본이 있으면 본문을 갱신하고 없으면 작성본을 복사해 생성
        invitationDocumentRepository
                .findByInvitationIdAndKind(invitationId, InvitationDocumentKind.PUBLISHED)
                .ifPresentOrElse(published -> {
                            // 기존 공개본을 덮어쓰기 전에 사진 키를 모으기
                            Set<String> removedPhotoKeys = collectPhotoKeys(invitationId, published.getBody());

                            // 이번에 공개할 작성본에도 남아 있는 사진은 보존
                            removedPhotoKeys.removeAll(
                                    collectPhotoKeys(invitationId, draftBody)
                            );

                            // 작성본을 이번 공개본으로 반영
                            published.updateBody(draftBody);

                            // 기존 공개본에서만 사용했던 사진을 같은 트랜잭션에서 예약한다.
                            if (!removedPhotoKeys.isEmpty()) {
                                photoCleanupOutbox.enqueue(removedPhotoKeys);
                            }
                        },
                        // 기존 공개본이 없을 경우(최초 공개)
                        () -> {
                            // 최초 공개는 작성본으로 새 공개본을 생성
                            invitationDocumentRepository.save(
                                    InvitationDocument.create(
                                            invitation,
                                            InvitationDocumentKind.PUBLISHED,
                                            draft.getTemplate(),
                                            draftBody
                                    )
                            );
                        }
                );

        // 공개 상태와 마지막 공개 시각을 갱신
        invitation.publish();
    }

    // 현재 청첩장의 공개 상태와 버전을 조회
    @Override
    public InvitationPublishResponseDto getPublishResult(
            Long userId,
            Long invitationId
    ) {
        Invitation invitation = invitationRepository.findByOwnerIdAndId(
                        userId,
                        invitationId
                )
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        return InvitationPublishResponseDto.from(invitation);
    }

    // 공개 상태인 청첩장의 공개본을 조회해 하객용 응답으로 변환
    @Override
    public PublicInvitationResponseDto getPublicInvitation(
            String publicId
    ) {
        // 청첩장의 공개본
        InvitationDocument published = invitationDocumentRepository.findPublishedByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 사진 저장 키를 CloudFront URL로 변환해 공개 본문에 반영
        JsonNode publicBody = publicBodyMapper.toPublicBody(
                published.getBody(),
                publicPhotoUrlResolver::resolve
        );

        return PublicInvitationResponseDto.of(
                published,
                publicBody
        );
    }

    // 공개 처리와 같은 청첩장 행을 잠그고 공개본 삭제와 상태 전환을 함께 처리
    @Transactional
    @Override
    public void unpublish(
            Long userId,
            Long invitationId
    ) {
        // 청첩장 소유권을 확인하고 공개 상태를 변경하는 동안 부모 행을 잠금
        Invitation invitation = invitationRepository
                .findByOwnerIdAndIdForUpdate(userId, invitationId)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 공개본이 있는 경우에만 사진 정리와 문서 삭제를 수행
        invitationDocumentRepository
                .findByInvitationIdAndKind(invitationId, InvitationDocumentKind.PUBLISHED)
                .ifPresent(published -> {
                    // 삭제할 공개본에서 사진 키를 모으기
                    Set<String> removedPhotoKeys = collectPhotoKeys(invitationId, published.getBody());

                    // 정리 후보가 있을 때만 현재 작성본을 조회
                    if (!removedPhotoKeys.isEmpty()) {
                        InvitationDocument draft = invitationDocumentRepository
                                .findByInvitationIdAndKind(invitationId, InvitationDocumentKind.DRAFT)
                                .orElseThrow(() -> new BusinessException(
                                        InvitationErrorCode.INVITATION_DRAFT_NOT_FOUND
                                ));

                        // 작성본에서 계속 사용하는 사진은 정리 후보에서 제외
                        removedPhotoKeys.removeAll(
                                collectPhotoKeys(invitationId, draft.getBody())
                        );

                        // 공개본에서만 사용하던 사진을 정리 예약
                        if (!removedPhotoKeys.isEmpty()) {
                            photoCleanupOutbox.enqueue(removedPhotoKeys);
                        }
                    }

                    invitationDocumentRepository.delete(published);
                });

        // 기존 동작대로 청첩장을 비공개 상태로 전환
        invitation.unpublish();
    }

    // 청첩장과 연결된 문서를 삭제하고, 두 문서에서 사용한 사진의 정리를 예약
    @Transactional
    @Override
    public void delete(
            Long userId,
            Long invitationId
    ) {
        // 소유권을 확인하고 삭제가 끝날 때까지 부모 행을 잠금
        Invitation invitation = invitationRepository
                .findByOwnerIdAndIdForUpdate(userId, invitationId)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 삭제할 작성본과 공개본을 조회
        List<InvitationDocument> documents = invitationDocumentRepository
                .findByInvitationId(invitationId);

        // 각 문서의 사진 키를 합쳐 두 문서에서 사용하는 같은 키를 한 번만 모은다.
        Set<String> storageKeys = new LinkedHashSet<>();

        for (InvitationDocument document : documents) {
            storageKeys.addAll(
                    collectPhotoKeys(invitationId, document.getBody())
            );
        }

        // 사진 정리 예약과 문서·청첩장 삭제를 같은 트랜잭션에서 처리
        photoCleanupOutbox.enqueue(storageKeys);

        invitationDocumentRepository.deleteAll(documents);

        invitationRepository.delete(invitation);
    }

    // 문서 하나에서 해당 청첩장에 속한 사진 키를 중복 없이 모으기
    private Set<String> collectPhotoKeys(
            Long invitationId,
            JsonNode body
    ) {
        Set<String> storageKeys = new LinkedHashSet<>();

        String photoPrefix = "invitations/" + invitationId + "/photos/";

        // 대표 사진과 방명록 사진의 키를 모은다.
        collectPhotoKey(
                body.path("coverPhoto"),
                photoPrefix,
                storageKeys
        );

        collectPhotoKey(
                body.path("guestbook").path("photo"),
                photoPrefix,
                storageKeys
        );

        // 갤러리 배열에 남아 있는 각 사진의 키를 모은다.
        for (JsonNode photo : body.path("gallery").path("photos")) {
            collectPhotoKey(
                    photo,
                    photoPrefix,
                    storageKeys
            );
        }

        return storageKeys;
    }

    // 해당 청첩장의 사진 경로에 속하는 객체 키만 수집
    private void collectPhotoKey(
            JsonNode photo,
            String photoPrefix,
            Set<String> storageKeys
    ) {
        JsonNode keyNode = photo.path("storageKey");

        if (!keyNode.isString()) {
            return;
        }

        String storageKey = keyNode.stringValue();

        if (storageKey.startsWith(photoPrefix)) {
            storageKeys.add(storageKey);
        }
    }
}
