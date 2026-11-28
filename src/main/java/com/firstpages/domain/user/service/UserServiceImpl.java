package com.firstpages.domain.user.service;

import com.firstpages.domain.invitation.entity.Invitation;
import com.firstpages.domain.invitation.repository.InvitationRepository;
import com.firstpages.domain.invitation.service.InvitationService;
import com.firstpages.domain.user.dto.response.UserMeResponseDto;
import com.firstpages.domain.user.entity.User;
import com.firstpages.domain.user.event.AccountWithdrawn;
import com.firstpages.domain.user.exception.UserErrorCode;
import com.firstpages.domain.user.port.SocialAccountUnlinkException;
import com.firstpages.domain.user.port.SocialAccountUnlinker;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 사용자 도메인의 비즈니스 로직을 구현하는 클래스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    // 회원 조회와 삭제에 사용하는 저장소
    private final UserRepository userRepository;

    // 탈퇴 회원의 소유 청첩장 조회에 사용하는 저장소
    private final InvitationRepository invitationRepository;

    // 청첩장 삭제와 사진 정리 예약에 사용하는 서비스
    private final InvitationService invitationService;

    // 외부 소셜 계정 연결 해제 계약
    private final SocialAccountUnlinker socialAccountUnlinker;

    // 커밋 후 Redis 정리를 연결하는 이벤트 발행기
    private final ApplicationEventPublisher eventPublisher;

    // 인증된 사용자 ID로 회원 정보를 조회하고 응답 DTO로 변환
    @Override
    public UserMeResponseDto getMyInfo(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return UserMeResponseDto.from(user);
    }

    // 카카오 연결 해제 확인 후 회원과 소유 청첩장 삭제
    @Transactional
    @Override
    public void withdraw(Long userId) {

        // 탈퇴와 토큰 발급이 겹치지 않도록 회원 행 잠금
        User user = userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        try {
            // 저장된 소셜 회원번호로 연결 해제 요청
            socialAccountUnlinker.unlink(
                    user.getProvider(),
                    user.getProviderId()
            );
        } catch (SocialAccountUnlinkException exception) {
            // 연결 해제 실패를 회원탈퇴 API 오류로 변환
            throw new BusinessException(UserErrorCode.KAKAO_UNLINK_FAILED);
        }

        // 소유 청첩장을 ID 순서로 잠금
        List<Invitation> invitations = invitationRepository.findAllByOwnerIdForUpdate(userId);

        // 청첩장 삭제 수행
        for (Invitation invitation : invitations) {
            invitationService.delete(
                    userId,
                    invitation.getId()
            );
        }

        // 청첩장 정리 후 회원 행 삭제
        userRepository.delete(user);

        // 커밋 성공 시 실행할 Redis 정리 등록
        eventPublisher.publishEvent(new AccountWithdrawn(userId));
    }
}
