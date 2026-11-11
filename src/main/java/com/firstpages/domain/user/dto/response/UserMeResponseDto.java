package com.firstpages.domain.user.dto.response;

import com.firstpages.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMeResponseDto {

    private final Long id;

    private final String nickname;

    public static UserMeResponseDto from(User user) {
        return new UserMeResponseDto(
                user.getId(),
                user.getNickname()
        );
    }
}
