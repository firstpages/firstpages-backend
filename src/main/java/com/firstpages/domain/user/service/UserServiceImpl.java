package com.firstpages.domain.user.service;

import com.firstpages.domain.user.dto.response.UserMeResponseDto;
import com.firstpages.domain.user.entity.User;
import com.firstpages.domain.user.exception.UserErrorCode;
import com.firstpages.domain.user.repository.UserRepository;
import com.firstpages.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 사용자 도메인의 비즈니스 로직을 구현하는 클래스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // 인증된 사용자 ID로 회원 정보를 조회하고 응답 DTO로 변환
    @Override
    public UserMeResponseDto getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(UserErrorCode.USER_NOT_FOUND)
                );

        return UserMeResponseDto.from(user);
    }
}
