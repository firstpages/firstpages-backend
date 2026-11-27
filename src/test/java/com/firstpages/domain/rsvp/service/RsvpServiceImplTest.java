package com.firstpages.domain.rsvp.service;

import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.entity.InvitationDocument;
import com.firstpages.domain.invitation.entity.InvitationDocumentKind;
import com.firstpages.domain.invitation.exception.InvitationErrorCode;
import com.firstpages.domain.invitation.repository.InvitationDocumentRepository;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.domain.rsvp.dto.request.RsvpCreateRequestDto;
import com.firstpages.domain.rsvp.entity.Rsvp;
import com.firstpages.domain.rsvp.entity.RsvpGuestSide;
import com.firstpages.domain.rsvp.exception.RsvpErrorCode;
import com.firstpages.domain.rsvp.repository.RsvpRepository;
import com.firstpages.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RSVP 제출 서비스")
class RsvpServiceImplTest {

    // 테스트 청첩장의 공유 식별자
    private static final String PUBLIC_ID = "0123456789abcdef0123456789abcdef";

    // 테스트 청첩장의 내부 식별자
    private static final Long INVITATION_ID = 42L;

    // 청첩장 잠금 조회 결과를 설정한다.
    @Mock
    private InvitationRepository invitationRepository;

    // 현재 공개본의 RSVP 설정을 제공한다.
    @Mock
    private InvitationDocumentRepository invitationDocumentRepository;

    // 중복 조회와 응답 저장 여부를 확인한다.
    @Mock
    private RsvpRepository rsvpRepository;

    // 제출 조건을 검증하는 실제 서비스를 사용한다.
    @InjectMocks
    private RsvpServiceImpl rsvpService;

    // 참석 응답이 청첩장과 연결되고 하객 정보와 인원이 저장되는지 확인한다.
    @Test
    @DisplayName("참석 응답에 동반 인원과 식사 인원을 저장한다")
    void savesAttendingResponse() {
        Invitation invitation = stubPublishedInvitation(true);

        rsvpService.create(PUBLIC_ID, request(true, 2, 3));

        ArgumentCaptor<Rsvp> saved = ArgumentCaptor.forClass(Rsvp.class);
        verify(rsvpRepository).save(saved.capture());
        assertThat(saved.getValue().getInvitation()).isSameAs(invitation);
        assertThat(saved.getValue().getGuestName()).isEqualTo("김하객");
        assertThat(saved.getValue().getGuestSide()).isEqualTo(RsvpGuestSide.GROOM);
        assertThat(saved.getValue().getPhoneLastFour()).isEqualTo("0123");
        assertThat(saved.getValue().isAttending()).isTrue();
        assertThat(saved.getValue().getCompanionCount()).isEqualTo(2);
        assertThat(saved.getValue().getMealCount()).isEqualTo(3);
    }

    // 불참 요청에서 생략하거나 0으로 제출한 인원은 모두 0으로 저장한다.
    @ParameterizedTest(name = "{displayName} [{index}]")
    @CsvSource(value = {"NULL, NULL", "0, 0", "NULL, 0", "0, NULL"}, nullValues = "NULL")
    @DisplayName("불참 응답의 동반 인원과 식사 인원이 생략되거나 0이면 저장한다")
    void savesNonAttendingResponse(Integer companionCount, Integer mealCount) {
        stubPublishedInvitation(true);

        rsvpService.create(PUBLIC_ID, request(false, companionCount, mealCount));

        ArgumentCaptor<Rsvp> saved = ArgumentCaptor.forClass(Rsvp.class);
        verify(rsvpRepository).save(saved.capture());
        assertThat(saved.getValue().isAttending()).isFalse();
        assertThat(saved.getValue().getCompanionCount()).isZero();
        assertThat(saved.getValue().getMealCount()).isZero();
    }

    // 참석과 불참 각각의 인원 규칙을 위반하면 저장하지 않는다.
    @ParameterizedTest(name = "{displayName} [{index}]")
    @CsvSource(value = {
            "false, 1, 0", "false, 0, 1", "false, 1, 1",
            "true, NULL, 0", "true, 0, NULL", "true, 1, 3"
    }, nullValues = "NULL")
    @DisplayName("참석 여부와 인원 조합이 맞지 않으면 저장을 거절한다")
    void rejectsInvalidCounts(boolean attending, Integer companionCount, Integer mealCount) {
        stubPublishedInvitation(true);

        assertThatThrownBy(() -> rsvpService.create(PUBLIC_ID, request(attending, companionCount, mealCount)))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(RsvpErrorCode.RSVP_INVALID_COUNTS));
        verifyNoInteractions(rsvpRepository);
    }

    // 현재 DTO가 허용하는 최대 정수에서도 본인 포함 계산이 오버플로하지 않는지 확인한다.
    @Test
    @DisplayName("동반 인원의 정수 최댓값에서도 총 참석 인원을 올바르게 비교한다")
    void calculatesTotalGuestsWithoutOverflow() {
        stubPublishedInvitation(true);

        rsvpService.create(PUBLIC_ID, request(true, Integer.MAX_VALUE, 1));

        verify(rsvpRepository).save(any(Rsvp.class));
    }

    // 이름은 앞뒤 공백을 제거한 뒤 같은 청첩장의 중복 제출을 판정한다.
    @Test
    @DisplayName("같은 이름·하객 측·전화번호 뒷 네 자리로 이미 제출했으면 저장하지 않는다")
    void rejectsDuplicateResponse() {
        stubPublishedInvitation(true);
        when(rsvpRepository.existsByInvitationIdAndGuestNameAndGuestSideAndPhoneLastFour(
                INVITATION_ID, "김하객", RsvpGuestSide.GROOM, "0123"
        )).thenReturn(true);

        assertThatThrownBy(() -> rsvpService.create(PUBLIC_ID, request(true, 0, 1)))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(RsvpErrorCode.RSVP_ALREADY_SUBMITTED));
        verify(rsvpRepository, never()).save(any(Rsvp.class));
    }

    // 비공개 청첩장은 공개본 조회와 응답 저장을 진행하지 않는다.
    @Test
    @DisplayName("비공개 청첩장에는 참석 응답을 제출할 수 없다")
    void rejectsPrivateInvitation() {
        Invitation invitation = Invitation.create(null, "테스트 청첩장", PUBLIC_ID);
        when(invitationRepository.findByPublicIdForUpdate(PUBLIC_ID)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> rsvpService.create(PUBLIC_ID, request(true, 0, 1)))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND));
        verifyNoInteractions(invitationDocumentRepository, rsvpRepository);
    }

    // 공개본에서 RSVP가 꺼져 있으면 저장하지 않는다.
    @Test
    @DisplayName("공개본에서 RSVP가 꺼져 있으면 제출을 거절한다")
    void rejectsDisabledRsvp() {
        stubPublishedInvitation(false);

        assertThatThrownBy(() -> rsvpService.create(PUBLIC_ID, request(true, 0, 1)))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(RsvpErrorCode.RSVP_DISABLED));
        verifyNoInteractions(rsvpRepository);
    }

    // 더보기 페이지와 별도로 전체 집계를 조회하고 응답이 없는 측을 0으로 채운다.
    @Test
    @DisplayName("다음 페이지를 조회해도 전체 응답을 집계하고 응답이 없는 측은 0을 반환한다")
    void listsPageWithInvitationWideSummary() {
        Long ownerId = 7L;
        PageRequest pageable = PageRequest.of(1, 20);
        RsvpRepository.SideSummary groom = mock(RsvpRepository.SideSummary.class);
        when(invitationRepository.existsByIdAndOwnerId(INVITATION_ID, ownerId)).thenReturn(true);
        when(rsvpRepository.findByInvitationIdOrderByCreatedAtDescIdDesc(INVITATION_ID, pageable))
                .thenReturn(Page.empty(pageable));
        when(rsvpRepository.summarizeByGuestSide(INVITATION_ID)).thenReturn(List.of(groom));
        when(groom.getGuestSide()).thenReturn(RsvpGuestSide.GROOM);
        when(groom.getAttendingGuests()).thenReturn(5L);
        when(groom.getDeclinedResponses()).thenReturn(2L);
        when(groom.getMealGuests()).thenReturn(4L);

        var result = rsvpService.getRsvps(ownerId, INVITATION_ID, 1, 20);

        assertThat(result.getResponses().getPage()).isEqualTo(1);
        assertThat(result.getResponses().getItems()).isEmpty();
        assertThat(result.getSummary().getGroom().getAttendingGuests()).isEqualTo(5);
        assertThat(result.getSummary().getGroom().getDeclinedResponses()).isEqualTo(2);
        assertThat(result.getSummary().getGroom().getMealGuests()).isEqualTo(4);
        assertThat(result.getSummary().getBride().getAttendingGuests()).isZero();
        assertThat(result.getSummary().getBride().getDeclinedResponses()).isZero();
        assertThat(result.getSummary().getBride().getMealGuests()).isZero();
        verifyNoInteractions(invitationDocumentRepository);
    }

    // 소유권 확인이 실패하면 응답 목록과 통계를 조회하지 않는다.
    @Test
    @DisplayName("청첩장 소유자가 아니면 응답 목록과 통계 조회를 거절한다")
    void rejectsNonOwnerListing() {
        Long requesterId = 8L;
        when(invitationRepository.existsByIdAndOwnerId(INVITATION_ID, requesterId)).thenReturn(false);

        assertThatThrownBy(() -> rsvpService.getRsvps(requesterId, INVITATION_ID, 0, 20))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND));
        verifyNoInteractions(rsvpRepository);
    }

    // 공개 상태와 공개본의 RSVP 활성화 값을 테스트에 맞게 설정한다.
    private Invitation stubPublishedInvitation(boolean enabled) {
        Invitation invitation = Invitation.create(null, "테스트 청첩장", PUBLIC_ID);
        ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
        invitation.publish();

        var body = JsonNodeFactory.instance.objectNode();
        body.putObject("rsvp").put("enabled", enabled);
        InvitationDocument published = InvitationDocument.create(
                invitation, InvitationDocumentKind.PUBLISHED, null, body
        );

        when(invitationRepository.findByPublicIdForUpdate(PUBLIC_ID)).thenReturn(Optional.of(invitation));
        when(invitationDocumentRepository.findByInvitationIdAndKind(INVITATION_ID, InvitationDocumentKind.PUBLISHED))
                .thenReturn(Optional.of(published));
        return invitation;
    }

    // 이름의 앞뒤 공백과 전화번호의 앞자리 0을 포함한 요청을 만든다.
    private RsvpCreateRequestDto request(boolean attending, Integer companionCount, Integer mealCount) {
        RsvpCreateRequestDto request = BeanUtils.instantiateClass(RsvpCreateRequestDto.class);
        ReflectionTestUtils.setField(request, "guestName", " 김하객 ");
        ReflectionTestUtils.setField(request, "guestSide", RsvpGuestSide.GROOM);
        ReflectionTestUtils.setField(request, "phoneLastFour", "0123");
        ReflectionTestUtils.setField(request, "attending", attending);
        ReflectionTestUtils.setField(request, "companionCount", companionCount);
        ReflectionTestUtils.setField(request, "mealCount", mealCount);
        return request;
    }
}
