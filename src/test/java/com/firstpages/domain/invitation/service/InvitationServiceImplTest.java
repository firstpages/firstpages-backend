package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.dto.request.InvitationCreateRequestDto;
import com.firstpages.domain.invitation.dto.response.InvitationCreateResponseDto;
import com.firstpages.domain.invitation.dto.response.InvitationDetailResponseDto;
import com.firstpages.domain.invitation.dto.response.InvitationListResponseDto;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.entity.InvitationStatus;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.domain.template.entity.Template;
import com.firstpages.domain.template.exception.TemplateErrorCode;
import com.firstpages.domain.template.repository.TemplateRepository;
import com.firstpages.domain.user.entity.SocialProvider;
import com.firstpages.domain.user.entity.User;
import com.firstpages.domain.user.exception.UserErrorCode;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 청첩장 서비스의 생성·목록·상세 조회 로직을 검증합니다.
@ExtendWith(MockitoExtension.class)
@DisplayName("청첩장 서비스 단위 테스트")
class InvitationServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long TEMPLATE_ID = 10L;
    private static final Long INVITATION_ID = 301L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationDocumentRepository invitationDocumentRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private InvitationDraftBodyFactory draftFactory;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    private User owner;
    private Template template;
    private Invitation invitation;
    private InvitationCreateRequestDto requestDto;
    private ObjectNode initialBody;

    // 각 테스트에서 사용할 엔티티와 요청 데이터를 준비합니다.
    @BeforeEach
    void setUp() {
        owner = User.create(
                SocialProvider.KAKAO,
                "kakao-user-1"
        );
        ReflectionTestUtils.setField(owner, "id", USER_ID);

        template = Template.create(
                "paper-cover",
                "페이퍼 커버",
                "템플릿 부제목",
                "템플릿 설명",
                "classic",
                List.of("심플"),
                "https://images.example.com/templates/paper-cover.jpg",
                "gowun-batang",
                JsonNodeFactory.instance.objectNode(),
                true
        );
        ReflectionTestUtils.setField(template, "id", TEMPLATE_ID);

        invitation = Invitation.create(
                owner,
                "",
                "a".repeat(32)
        );

        ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
        ReflectionTestUtils.setField(invitation, "version", 0);
        ReflectionTestUtils.setField(
                invitation,
                "createdAt",
                LocalDateTime.of(2026, 11, 20, 12, 0)
        );
        ReflectionTestUtils.setField(
                invitation,
                "updatedAt",
                LocalDateTime.of(2026, 11, 20, 12, 0)
        );

        requestDto = BeanUtils.instantiateClass(
                InvitationCreateRequestDto.class
        );
        ReflectionTestUtils.setField(
                requestDto,
                "templateId",
                TEMPLATE_ID
        );

        initialBody = JsonNodeFactory.instance.objectNode();
        initialBody.put("font", "gowun-batang");
        initialBody.putObject("basic").putNull("weddingDate");
        initialBody.putNull("coverPhoto");
    }

    // 정상 요청이면 청첩장과 초기 작성본을 저장합니다.
    @Test
    @DisplayName("선택 가능한 템플릿으로 청첩장과 작성본을 생성한다")
    void createSuccess() {
        // given
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(owner));

        when(templateRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.of(template));

        when(invitationRepository.save(any(Invitation.class)))
                .thenReturn(invitation);

        when(draftFactory.create(template))
                .thenReturn(initialBody);

        // when
        InvitationCreateResponseDto result =
                invitationService.create(USER_ID, requestDto);

        // then
        ArgumentCaptor<Invitation> invitationCaptor =
                ArgumentCaptor.forClass(Invitation.class);

        ArgumentCaptor<InvitationDocument> documentCaptor =
                ArgumentCaptor.forClass(InvitationDocument.class);

        verify(invitationRepository).save(invitationCaptor.capture());
        verify(invitationDocumentRepository).save(documentCaptor.capture());

        Invitation savedArgument = invitationCaptor.getValue();
        InvitationDocument savedDocument = documentCaptor.getValue();

        assertAll(
                () -> assertEquals(INVITATION_ID, result.getId()),
                () -> assertEquals(
                        InvitationStatus.DRAFT,
                        result.getStatus()
                ),
                () -> assertSame(owner, savedArgument.getOwner()),
                () -> assertEquals(
                        "",
                        savedArgument.getManagementTitle()
                ),
                () -> assertEquals(
                        InvitationStatus.DRAFT,
                        savedArgument.getStatus()
                ),
                () -> assertNotNull(savedArgument.getPublicId())
        );

        assertTrue(
                savedArgument.getPublicId().matches("[0-9a-f]{32}")
        );

        assertAll(
                () -> assertSame(
                        invitation,
                        savedDocument.getInvitation()
                ),
                () -> assertSame(
                        template,
                        savedDocument.getTemplate()
                ),
                () -> assertEquals(
                        InvitationDocumentKind.DRAFT,
                        savedDocument.getKind()
                ),
                () -> assertEquals(
                        initialBody,
                        savedDocument.getBody()
                )
        );

        verify(draftFactory).create(template);
    }

    // 생성 시 소유자가 없으면 저장을 수행하지 않습니다.
    @Test
    @DisplayName("청첩장 생성 시 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void createFailsWhenUserNotFound() {
        // given
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> invitationService.create(USER_ID, requestDto)
        );

        // then
        assertEquals(
                UserErrorCode.USER_NOT_FOUND,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                templateRepository,
                invitationRepository,
                invitationDocumentRepository,
                draftFactory
        );
    }

    // 템플릿이 없으면 청첩장을 저장하지 않습니다.
    @Test
    @DisplayName("템플릿이 없으면 TEMPLATE_NOT_FOUND 예외가 발생한다")
    void createFailsWhenTemplateNotFound() {
        // given
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(owner));

        when(templateRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> invitationService.create(USER_ID, requestDto)
        );

        // then
        assertEquals(
                TemplateErrorCode.TEMPLATE_NOT_FOUND,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                invitationRepository,
                invitationDocumentRepository,
                draftFactory
        );
    }

    // 선택 불가능한 템플릿이면 청첩장을 저장하지 않습니다.
    @Test
    @DisplayName("선택 불가 템플릿이면 TEMPLATE_NOT_SELECTABLE 예외가 발생한다")
    void createFailsWhenTemplateNotSelectable() {
        // given
        ReflectionTestUtils.setField(
                template,
                "selectable",
                false
        );

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(owner));

        when(templateRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.of(template));

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> invitationService.create(USER_ID, requestDto)
        );

        // then
        assertEquals(
                TemplateErrorCode.TEMPLATE_NOT_SELECTABLE,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                invitationRepository,
                invitationDocumentRepository,
                draftFactory
        );
    }

    // 작성본 저장 실패 예외를 호출자에게 전달합니다.
    @Test
    @DisplayName("작성본 저장에 실패하면 발생한 예외를 그대로 전달한다")
    void createPropagatesDocumentSaveFailure() {
        // given
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(owner));

        when(templateRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.of(template));

        when(invitationRepository.save(any(Invitation.class)))
                .thenReturn(invitation);

        when(draftFactory.create(template))
                .thenReturn(initialBody);

        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("작성본 저장 실패");

        when(invitationDocumentRepository.save(
                any(InvitationDocument.class)
        )).thenThrow(failure);

        // when
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> invitationService.create(USER_ID, requestDto)
        );

        // then
        assertSame(failure, exception);

        verify(invitationRepository).save(any(Invitation.class));
    }

    // 조회 결과가 없으면 빈 목록을 반환합니다.
    @Test
    @DisplayName("소유한 청첩장이 없으면 빈 목록을 반환한다")
    void getInvitationsReturnsEmptyList() {
        // given
        when(invitationDocumentRepository.findAllDraftsByOwnerId(USER_ID))
                .thenReturn(List.of());

        // when
        List<InvitationListResponseDto> result =
                invitationService.getInvitations(USER_ID);

        // then
        assertTrue(result.isEmpty());

        verify(invitationDocumentRepository)
                .findAllDraftsByOwnerId(USER_ID);

        verifyNoInteractions(userRepository);
    }

    // 초기 작성본의 미작성 항목을 null로 반환합니다.
    @Test
    @DisplayName("초기 청첩장 목록의 예식일과 대표 사진은 null이다")
    void getInvitationsReturnsInitialDraft() {
        // given
        InvitationDocument draft = InvitationDocument.create(
                invitation,
                InvitationDocumentKind.DRAFT,
                template,
                initialBody
        );

        when(invitationDocumentRepository.findAllDraftsByOwnerId(USER_ID))
                .thenReturn(List.of(draft));

        // when
        List<InvitationListResponseDto> result =
                invitationService.getInvitations(USER_ID);

        // then
        assertEquals(1, result.size());

        InvitationListResponseDto item = result.getFirst();

        assertAll(
                () -> assertEquals(INVITATION_ID, item.getId()),
                () -> assertEquals("", item.getManagementTitle()),
                () -> assertEquals(
                        "페이퍼 커버",
                        item.getTemplateName()
                ),
                () -> assertEquals(
                        InvitationStatus.DRAFT,
                        item.getStatus()
                ),
                () -> assertNull(item.getWeddingDate()),
                () -> assertNull(item.getCoverPhoto())
        );
    }

    // 작성본의 편집 내용과 부모 청첩장의 공개 상태를 함께 반환합니다.
    @Test
    @DisplayName("공개된 청첩장도 작성본의 날짜와 사진으로 목록에 표시한다")
    void getInvitationsReturnsEditedDraftOfPublishedInvitation() {
        // given
        ReflectionTestUtils.setField(
                invitation,
                "status",
                InvitationStatus.PUBLISHED
        );

        invitation.updateManagementTitle("지훈과 서연의 결혼식");

        ObjectNode editedBody = initialBody.deepCopy();

        editedBody.withObject("/basic")
                .put("weddingDate", "2027-05-22");

        editedBody.putObject("coverPhoto")
                .put("storageKey", "invitations/301/cover.jpg")
                .put("url", "https://images.example.com/cover.jpg")
                .put("focalX", 50)
                .put("focalY", 40)
                .put("zoom", 1.2);

        InvitationDocument draft = InvitationDocument.create(
                invitation,
                InvitationDocumentKind.DRAFT,
                template,
                editedBody
        );

        when(invitationDocumentRepository.findAllDraftsByOwnerId(USER_ID))
                .thenReturn(List.of(draft));

        // when
        List<InvitationListResponseDto> result =
                invitationService.getInvitations(USER_ID);

        // then
        assertEquals(1, result.size());

        InvitationListResponseDto item = result.getFirst();

        assertAll(
                () -> assertEquals(
                        "지훈과 서연의 결혼식",
                        item.getManagementTitle()
                ),
                () -> assertEquals(
                        InvitationStatus.PUBLISHED,
                        item.getStatus()
                ),
                () -> assertEquals(
                        LocalDate.of(2027, 5, 22),
                        item.getWeddingDate()
                ),
                () -> assertNotNull(item.getCoverPhoto())
        );

        assertAll(
                () -> assertEquals(
                        "https://images.example.com/cover.jpg",
                        item.getCoverPhoto().getUrl()
                ),
                () -> assertEquals(
                        Integer.valueOf(50),
                        item.getCoverPhoto().getFocalX()
                ),
                () -> assertEquals(
                        Integer.valueOf(40),
                        item.getCoverPhoto().getFocalY()
                ),
                () -> assertEquals(
                        1.2,
                        item.getCoverPhoto().getZoom(),
                        0.0001
                )
        );
    }

    // 소유자 조건에 일치하는 작성본을 상세 응답으로 반환합니다.
    @Test
    @DisplayName("본인 청첩장의 작성본을 편집용 상세 응답으로 반환한다")
    void getDetailSuccess() {
        // given
        InvitationDocument draft = InvitationDocument.create(
                invitation,
                InvitationDocumentKind.DRAFT,
                template,
                initialBody
        );

        when(invitationDocumentRepository.findDraftByInvitationIdAndOwnerId(
                INVITATION_ID,
                USER_ID
        )).thenReturn(Optional.of(draft));

        // when
        InvitationDetailResponseDto result =
                invitationService.getDetail(USER_ID, INVITATION_ID);

        // then
        assertAll(
                () -> assertEquals(INVITATION_ID, result.getId()),
                () -> assertEquals("", result.getManagementTitle()),
                () -> assertEquals(TEMPLATE_ID, result.getTemplateId()),
                () -> assertEquals(
                        "paper-cover",
                        result.getTemplateCode()
                ),
                () -> assertEquals(
                        "페이퍼 커버",
                        result.getTemplateName()
                ),
                () -> assertEquals(
                        InvitationStatus.DRAFT,
                        result.getStatus()
                ),
                () -> assertEquals(
                        invitation.getPublicId(),
                        result.getPublicId()
                ),
                () -> assertEquals(
                        Integer.valueOf(0),
                        result.getVersion()
                ),
                () -> assertEquals(
                        invitation.getCreatedAt(),
                        result.getCreatedAt()
                ),
                () -> assertEquals(
                        invitation.getUpdatedAt(),
                        result.getUpdatedAt()
                ),
                () -> assertEquals(initialBody, result.getBody())
        );

        verify(invitationDocumentRepository)
                .findDraftByInvitationIdAndOwnerId(
                        INVITATION_ID,
                        USER_ID
                );

        verifyNoInteractions(userRepository);
    }

    // 소유자 조건에 일치하는 작성본이 없으면 조회 실패로 처리합니다.
    @Test
    @DisplayName("조회 가능한 작성본이 없으면 INVITATION_NOT_FOUND 예외가 발생한다")
    void getDetailFailsWhenOwnedDraftNotFound() {
        // given
        when(invitationDocumentRepository.findDraftByInvitationIdAndOwnerId(
                INVITATION_ID,
                USER_ID
        )).thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> invitationService.getDetail(
                        USER_ID,
                        INVITATION_ID
                )
        );

        // then
        assertEquals(
                InvitationErrorCode.INVITATION_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(invitationDocumentRepository)
                .findDraftByInvitationIdAndOwnerId(
                        INVITATION_ID,
                        USER_ID
                );
    }
}