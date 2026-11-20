package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.dto.request.InvitationCreateRequestDto;
import com.firstpages.domain.invitation.dto.response.InvitationCreateResponseDto;
import com.firstpages.domain.invitation.dto.response.InvitationDetailResponseDto;
import com.firstpages.domain.invitation.dto.response.InvitationListResponseDto;

import java.util.List;

// 청첩장 비즈니스 로직 인터페이스
public interface InvitationService {

    // 선택한 템플릿으로 청첩장과 초기 작성본을 생성
    InvitationCreateResponseDto create(
            Long userId,
            InvitationCreateRequestDto requestDto
    );

    // 로그인 사용자의 청첩장 목록을 조회
    List<InvitationListResponseDto> getInvitations(
            Long userId
    );

    // 로그인 사용자가 소유한 청첩장의 편집용 상세 정보를 조회
    InvitationDetailResponseDto getDetail(
            Long userId,
            Long invitationId
    );
}
