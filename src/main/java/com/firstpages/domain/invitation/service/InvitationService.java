package com.firstpages.domain.invitation.service;

import com.firstpages.domain.invitation.dto.request.InvitationCreateRequestDto;
import com.firstpages.domain.invitation.dto.request.InvitationPublishRequestDto;
import com.firstpages.domain.invitation.dto.request.InvitationUpdateRequestDto;
import com.firstpages.domain.invitation.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    // 로그인 사용자가 소유한 청첩장의 관리용 제목과 작성본을 저장
    void update(
            Long userId,
            Long invitationId,
            InvitationUpdateRequestDto requestDto
    );

    // 로그인 사용자가 관리용 제목과 작성본을 첨부 사진과 함께 저장
    void update(
            Long userId,
            Long invitationId,
            InvitationUpdateRequestDto requestDto,
            Map<String, List<MultipartFile>> photoParts
    );

    // 소유한 청첩장의 ID와 현재 버전 조회
    InvitationUpdateResponseDto getUpdateResult(
            Long userId,
            Long invitationId
    );

    // 소유한 청첩장의 작성본을 공개본에 반영
    void publish(
            Long userId,
            Long invitationId,
            InvitationPublishRequestDto requestDto
    );

    // 소유한 청첩장의 현재 공개 결과를 조회
    InvitationPublishResponseDto getPublishResult(
            Long userId,
            Long invitationId
    );

    // 공유 식별자로 하객용 공개 청첩장을 조회
    PublicInvitationResponseDto getPublicInvitation(
            String publicId
    );

    // 소유한 청첩장의 공개본을 삭제하고 공개를 해제(비공개)
    void unpublish(
            Long userId,
            Long invitationId
    );

    // 소유한 청첩장과 작성본 및 공개본을 모두 삭제
    void delete(
            Long userId,
            Long invitationId
    );
}
