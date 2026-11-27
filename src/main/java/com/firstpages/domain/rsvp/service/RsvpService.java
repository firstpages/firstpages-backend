package com.firstpages.domain.rsvp.service;

import com.firstpages.domain.rsvp.dto.request.RsvpCreateRequestDto;
import com.firstpages.domain.rsvp.dto.request.RsvpUpdateRequestDto;
import com.firstpages.domain.rsvp.dto.response.RsvpItemResponseDto;
import com.firstpages.domain.rsvp.dto.response.RsvpListResponseDto;

// 하객의 참석 응답 제출 기능을 정의
public interface RsvpService {

    // 참석 응답이 활성화된 공개 청첩장에 하객의 응답을 등록
    void create(
            String publicId,
            RsvpCreateRequestDto requestDto
    );

    // 소유자의 청첩장에 제출된 응답 페이지와 전체 인원 집계를 조회
    RsvpListResponseDto getRsvps(
            Long userId,
            Long invitationId,
            int page,
            int size
    );

    // 소유자가 관리하는 청첩장의 RSVP 응답을 수정
    RsvpItemResponseDto update(
            Long userId,
            Long invitationId,
            Long rsvpId,
            RsvpUpdateRequestDto requestDto
    );

    // 소유자가 관리하는 청첩장의 RSVP 응답 한 건을 삭제
    void delete(
            Long userId,
            Long invitationId,
            Long rsvpId
    );
}
