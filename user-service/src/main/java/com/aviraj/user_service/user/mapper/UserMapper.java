package com.aviraj.user_service.user.mapper;

import com.aviraj.user_service.user.dto.UserRequestDto;
import com.aviraj.user_service.user.dto.UserResponseDto;
import com.aviraj.user_service.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class UserMapper{

    public UserResponseDto toResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        return dto;
    }

    public List<UserResponseDto> toResponseDto(List<User> users) {
        List<UserResponseDto> dto = new ArrayList<>();
        for(User user: users){
            UserResponseDto resUser = new UserResponseDto();
            resUser.setId(user.getId());
            resUser.setName(user.getName());
            resUser.setEmail(user.getEmail());
            dto.add(resUser);
        }

        return dto;
    }

    public User toUser(UserRequestDto dto){
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return user;
    }
}