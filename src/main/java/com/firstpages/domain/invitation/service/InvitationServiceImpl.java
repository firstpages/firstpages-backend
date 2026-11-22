package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.dto.request.InvitationCreateRequestDto;
import com.firstpages.domain.invitation.dto.request.InvitationPublishRequestDto;
import com.firstpages.domain.invitation.dto.request.InvitationUpdateRequestDto;
import com.firstpages.domain.invitation.dto.response.*;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.mapper.InvitationPublicBodyMapper;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
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
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
                .map(InvitationListResponseDto::from)
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

        return InvitationDetailResponseDto.from(draft);
    }

    // 소유권과 버전을 확인하고 청첩장 제목 및 작성본을 함께 저장
    @Transactional
    @Override
    public void update(
            Long userId,
            Long invitationId,
            InvitationUpdateRequestDto requestDto
    ) {
        // 작성본은 JSON 객체만 허용
        if (requestDto.getBody() == null || !requestDto.getBody().isObject()) {
            throw new BusinessException(
                    InvitationErrorCode.INVITATION_INVALID_BODY
            );
        }

        // 소유자의 청첩장을 조회하면서 부모의 낙관적 락 적용
        Invitation invitation = invitationRepository.findByIdAndOwnerIdWithOptimisticLock(invitationId, userId)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 요청에 담긴 버전과 현재 DB에서 조회한 버전이 다르면 예외 발생
        if (!Objects.equals(
                // 사용자가 가지고 있던 과거 버전
                requestDto.getVersion(),
                // 디비 조회로 가지고 온 최신 버전
                invitation.getVersion()
        )) {
            throw new BusinessException(
                    InvitationErrorCode.INVITATION_VERSION_CONFLICT
            );
        }

        // 소유권이 확인된 청첩장의 작성본 조회
        InvitationDocument document = invitationDocumentRepository.findByInvitationIdAndKind(invitationId, InvitationDocumentKind.DRAFT)
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_DRAFT_NOT_FOUND
                ));

        // 관리용 제목 변경
        invitation.updateManagementTitle(requestDto.getManagementTitle());

        // 전체 작성본 JSON 변경
        document.updateBody(requestDto.getBody());
    }

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

    // 소유권과 버전을 확인하고 작성본을 공개본에 반영
    @Transactional
    @Override
    public void publish(
            Long userId,
            Long invitationId,
            InvitationPublishRequestDto requestDto
    ) {
        // 소유한 청첩장의 부모 행을 잠금
        Invitation invitation = invitationRepository.findByOwnerIdAndIdForUpdate(
                        userId,
                        invitationId
                )
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_NOT_FOUND
                ));

        // 잠금을 얻은 뒤 요청 버전과 현재 버전을 비교
        if (!Objects.equals(
                requestDto.getVersion(),
                invitation.getVersion()
        )) {
            throw new BusinessException(
                    InvitationErrorCode.INVITATION_VERSION_CONFLICT
            );
        }

        // 공개에 사용할 저장된 작성본을 조회
        InvitationDocument draft = invitationDocumentRepository.findByInvitationIdAndKind(
                        invitationId,
                        InvitationDocumentKind.DRAFT
                )
                .orElseThrow(() -> new BusinessException(
                        InvitationErrorCode.INVITATION_DRAFT_NOT_FOUND
                ));

        // 공개본이 있으면 본문을 갱신하고 없으면 작성본을 복사해 생성
        invitationDocumentRepository.findByInvitationIdAndKind(
                        invitationId,
                        InvitationDocumentKind.PUBLISHED
                )
                .ifPresentOrElse(
                        published -> published.updateBody(draft.getBody()),
                        () -> invitationDocumentRepository.save(
                                InvitationDocument.create(
                                        invitation,
                                        InvitationDocumentKind.PUBLISHED,
                                        draft.getTemplate(),
                                        draft.getBody()
                                )
                        )
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

        // 사진 URL 조회 기능을 연결하기 전에는 본문만 가공
        JsonNode publicBody = publicBodyMapper.toPublicBody(
                published.getBody(),
                storageKey -> Optional.empty()
        );

        return PublicInvitationResponseDto.of(
                published,
                publicBody
        );
    }
}
