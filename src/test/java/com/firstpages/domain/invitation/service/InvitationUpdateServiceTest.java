package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.dto.request.InvitationUpdateRequestDto;
import com.firstpages.domain.invitation.dto.response.InvitationUpdateResponseDto;
import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.domain.template.repository.TemplateRepository;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// 청첩장 작성본 저장과 저장 결과 조회의 서비스 동작을 검증
@ExtendWith(MockitoExtension.class)
class InvitationUpdateServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long INVITATION_ID = 301L;
    private static final Integer VERSION = 3;
    private static final String MANAGEMENT_TITLE = "서준과 다은의 결혼식";

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationDocumentRepository invitationDocumentRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private InvitationDraftBodyFactory draftBodyFactory;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    // 정상 요청이면 소유권과 버전을 확인하고 제목 및 작성본 변경을 요청하는지 검증
    @Test
    @DisplayName("청첩장 저장에 성공하면 관리용 제목과 작성본을 변경한다")
    void updateSuccess() {
        // given
        ObjectNode body = createBody();
        InvitationUpdateRequestDto requestDto = createRequest(
                MANAGEMENT_TITLE,
                body,
                VERSION
        );

        Invitation invitation = mock(Invitation.class);
        InvitationDocument document = mock(InvitationDocument.class);

        when(invitationRepository.findByIdAndOwnerIdWithOptimisticLock(
                INVITATION_ID,
                USER_ID
        )).thenReturn(Optional.of(invitation));

        when(invitation.getVersion()).thenReturn(VERSION);

        when(invitationDocumentRepository.findByInvitationIdAndKind(
                INVITATION_ID,
                InvitationDocumentKind.DRAFT
        )).thenReturn(Optional.of(document));

        // when
        invitationService.update(USER_ID, INVITATION_ID, requestDto);

        // then
        verify(invitation).updateManagementTitle(MANAGEMENT_TITLE);
        verify(document).updateBody(body);

        // 변경 감지를 사용하므로 별도의 save 호출은 필요하지 않음
        verify(invitationRepository, never()).save(any(Invitation.class));
        verify(invitationDocumentRepository, never())
                .save(any(InvitationDocument.class));
    }

    // 본문이 null이면 데이터 조회 전에 도메인 예외가 발생하는지 검증
    @Test
    @DisplayName("작성본이 null이면 작성본 형식 오류가 발생한다")
    void updateWithNullBody() {
        // given
        InvitationUpdateRequestDto requestDto = createRequest(
                MANAGEMENT_TITLE,
                null,
                VERSION
        );

        // when & then
        assertThatThrownBy(() ->
                invitationService.update(USER_ID, INVITATION_ID, requestDto)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(InvitationErrorCode.INVITATION_INVALID_BODY)
        );

        verifyNoInteractions(
                invitationRepository,
                invitationDocumentRepository
        );
    }

    // JSON 배열은 작성본 객체로 허용하지 않는지 검증
    @Test
    @DisplayName("작성본이 JSON 배열이면 작성본 형식 오류가 발생한다")
    void updateWithArrayBody() {
        // given
        InvitationUpdateRequestDto requestDto = createRequest(
                MANAGEMENT_TITLE,
                JsonNodeFactory.instance.arrayNode(),
                VERSION
        );

        // when & then
        assertThatThrownBy(() ->
                invitationService.update(USER_ID, INVITATION_ID, requestDto)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(InvitationErrorCode.INVITATION_INVALID_BODY)
        );

        verifyNoInteractions(
                invitationRepository,
                invitationDocumentRepository
        );
    }

    // 청첩장이 없거나 소유권이 없으면 동일한 조회 실패 예외를 반환하는지 검증
    @Test
    @DisplayName("소유한 청첩장이 없으면 청첩장 조회 실패 예외가 발생한다")
    void updateInvitationNotFound() {
        // given
        InvitationUpdateRequestDto requestDto = createRequest(
                MANAGEMENT_TITLE,
                createBody(),
                VERSION
        );

        when(invitationRepository.findByIdAndOwnerIdWithOptimisticLock(
                INVITATION_ID,
                USER_ID
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                invitationService.update(USER_ID, INVITATION_ID, requestDto)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND)
        );

        verifyNoInteractions(invitationDocumentRepository);
    }

    // 요청 버전이 현재 버전과 다르면 작성본 조회와 변경을 수행하지 않는지 검증
    @Test
    @DisplayName("요청 버전이 현재 버전과 다르면 버전 충돌 예외가 발생한다")
    void updateVersionConflict() {
        // given
        InvitationUpdateRequestDto requestDto = createRequest(
                MANAGEMENT_TITLE,
                createBody(),
                VERSION
        );

        Invitation invitation = mock(Invitation.class);

        when(invitationRepository.findByIdAndOwnerIdWithOptimisticLock(
                INVITATION_ID,
                USER_ID
        )).thenReturn(Optional.of(invitation));

        when(invitation.getVersion()).thenReturn(VERSION + 1);

        // when & then
        assertThatThrownBy(() ->
                invitationService.update(USER_ID, INVITATION_ID, requestDto)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                InvitationErrorCode.INVITATION_VERSION_CONFLICT
                        )
        );

        verify(invitation, never()).updateManagementTitle(anyString());
        verifyNoInteractions(invitationDocumentRepository);
    }

    // 작성본이 없으면 제목 변경 전에 예외가 발생하는지 검증
    @Test
    @DisplayName("청첩장 작성본이 없으면 작성본 조회 실패 예외가 발생한다")
    void updateDraftNotFound() {
        // given
        InvitationUpdateRequestDto requestDto = createRequest(
                MANAGEMENT_TITLE,
                createBody(),
                VERSION
        );

        Invitation invitation = mock(Invitation.class);

        when(invitationRepository.findByIdAndOwnerIdWithOptimisticLock(
                INVITATION_ID,
                USER_ID
        )).thenReturn(Optional.of(invitation));

        when(invitation.getVersion()).thenReturn(VERSION);

        when(invitationDocumentRepository.findByInvitationIdAndKind(
                INVITATION_ID,
                InvitationDocumentKind.DRAFT
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                invitationService.update(USER_ID, INVITATION_ID, requestDto)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                InvitationErrorCode.INVITATION_DRAFT_NOT_FOUND
                        )
        );

        verify(invitation, never()).updateManagementTitle(anyString());
    }

    // 결과 조회 시 사용자와 청첩장 식별자를 올바른 순서로 전달하는지 검증
    @Test
    @DisplayName("저장 결과 조회에 성공하면 청첩장 ID와 현재 버전을 반환한다")
    void getUpdateResultSuccess() {
        // given
        Invitation invitation = mock(Invitation.class);

        when(invitationRepository.findByOwnerIdAndId(
                USER_ID,
                INVITATION_ID
        )).thenReturn(Optional.of(invitation));

        when(invitation.getId()).thenReturn(INVITATION_ID);
        when(invitation.getVersion()).thenReturn(VERSION + 1);

        // when
        InvitationUpdateResponseDto responseDto =
                invitationService.getUpdateResult(USER_ID, INVITATION_ID);

        // then
        assertThat(responseDto.getId()).isEqualTo(INVITATION_ID);
        assertThat(responseDto.getVersion()).isEqualTo(VERSION + 1);

        verify(invitationRepository).findByOwnerIdAndId(
                USER_ID,
                INVITATION_ID
        );
    }

    // 결과 조회에서도 소유권 조건을 만족하지 않으면 예외가 발생하는지 검증
    @Test
    @DisplayName("저장 결과를 조회할 청첩장이 없으면 조회 실패 예외가 발생한다")
    void getUpdateResultNotFound() {
        // given
        when(invitationRepository.findByOwnerIdAndId(
                USER_ID,
                INVITATION_ID
        )).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                invitationService.getUpdateResult(USER_ID, INVITATION_ID)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND)
        );
    }

    // 저장 테스트에 사용할 요청 DTO를 생성
    private InvitationUpdateRequestDto createRequest(
            String managementTitle,
            JsonNode body,
            Integer version
    ) {
        InvitationUpdateRequestDto requestDto =
                newRequestInstance();

        ReflectionTestUtils.setField(
                requestDto,
                "managementTitle",
                managementTitle
        );
        ReflectionTestUtils.setField(requestDto, "body", body);
        ReflectionTestUtils.setField(requestDto, "version", version);

        return requestDto;
    }

    // 보호된 기본 생성자를 가진 요청 DTO의 테스트 인스턴스를 생성
    private InvitationUpdateRequestDto newRequestInstance() {
        return org.springframework.beans.BeanUtils.instantiateClass(
                InvitationUpdateRequestDto.class
        );
    }

    // 현재 서비스가 허용하는 JSON 객체 형태의 작성본을 생성
    private ObjectNode createBody() {
        ObjectNode body = JsonNodeFactory.instance.objectNode();

        body.put("font", "gowun-batang");
        body.putObject("basic")
                .put("groomName", "박서준")
                .put("brideName", "김다은");

        return body;
    }
}