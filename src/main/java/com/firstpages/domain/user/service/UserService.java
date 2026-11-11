package com.firstpages.domain.user.service;

import com.firstpages.domain.user.dto.response.UserMeResponseDto;
import org.springframework.stereotype.Service;

// 사용자 도메인의 서비스 기능을 정의하는 인터페이스
public interface UserService {

    // 현재 로그인한 사용자의 정보를 조회
    UserMeResponseDto getMyInfo(Long userId);
}
