package com.firstpages.domain.user.service;

import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.domain.invitation.service.InvitationService;
import com.firstpages.domain.user.entity.SocialProvider;
import com.firstpages.domain.user.entity.User;
import com.firstpages.domain.user.event.AccountWithdrawn;
import com.firstpages.domain.user.exception.UserErrorCode;
import com.firstpages.domain.user.port.SocialAccountUnlinkException;
import com.firstpages.domain.user.port.SocialAccountUnlinker;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

// 회원탈퇴 순서와 실패 시 후속 처리 차단 검증
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long USER_ID = 9L;
    private static final String PROVIDER_ID = "12345";

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationService invitationService;

    @Mock
    private SocialAccountUnlinker unlinker;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("카카오 연결 해제 후 소유 청첩장과 회원을 삭제하고 이벤트를 발행한다")
    void withdrawInOrder() {
        User user = givenKakaoUser();
        Invitation invitation = mock(Invitation.class);
        when(invitation.getId()).thenReturn(31L);
        when(invitationRepository.findAllByOwnerIdForUpdate(USER_ID)).thenReturn(List.of(invitation));

        userService.withdraw(USER_ID);

        // 회원 잠금부터 후속 정리 이벤트까지 호출 순서 검증
        InOrder order = inOrder(userRepository, unlinker, invitationRepository, invitationService, eventPublisher);
        order.verify(userRepository).findByIdForUpdate(USER_ID);
        order.verify(unlinker).unlink(SocialProvider.KAKAO, PROVIDER_ID);
        order.verify(invitationRepository).findAllByOwnerIdForUpdate(USER_ID);
        order.verify(invitationService).delete(USER_ID, 31L);
        order.verify(userRepository).delete(user);
        ArgumentCaptor<AccountWithdrawn> event = ArgumentCaptor.forClass(AccountWithdrawn.class);
        order.verify(eventPublisher).publishEvent(event.capture());
        assertEquals(USER_ID, event.getValue().getUserId());
    }

    @Test
    @DisplayName("카카오 연결 해제 실패 시 회원과 청첩장을 삭제하지 않는다")
    void abortsWhenUnlinkFails() {
        givenKakaoUser();
        doThrow(new SocialAccountUnlinkException("Kakao unavailable"))
                .when(unlinker).unlink(SocialProvider.KAKAO, PROVIDER_ID);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> userService.withdraw(USER_ID)
        );

        assertEquals(UserErrorCode.KAKAO_UNLINK_FAILED, exception.getErrorCode());
        verify(userRepository, never()).delete(any());
        verifyNoInteractions(invitationRepository, invitationService, eventPublisher);
    }

    // 잠금 조회에서 반환할 카카오 회원 준비
    private User givenKakaoUser() {
        User user = User.create(SocialProvider.KAKAO, PROVIDER_ID, "테스트 회원");
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        return user;
    }
}
