package com.firstpages.domain.guestbook.service;

import com.firstpages.domain.guestbook.dto.request.GuestbookCreateRequestDto;
import com.firstpages.domain.guestbook.dto.response.GuestbookItemResponseDto;
import org.springframework.data.domain.Page;

// 방명록 메시지 작성 기능을 정의
public interface GuestbookService {

    // 방명록이 활성화된 공개 청첩장에 축하 메시지를 등록
    void create(
            String publicId,
            GuestbookCreateRequestDto requestDto
    );

    // 소유자가 관리하는 청첩장의 방명록 메시지를 최신순으로 조회
    Page<GuestbookItemResponseDto> getGuestbooks(
            Long userId,
            Long invitationId,
            int page,
            int size
    );

    // 소유자가 관리하는 청첩장의 방명록 메시지 한 건을 삭제
    void delete(
            Long userId,
            Long invitationId,
            Long guestbookId
    );
}
