package com.aviraj.user_service.user.service;

import com.aviraj.user_service.user.dto.UserRequestDto;
import com.aviraj.user_service.user.dto.UserResponseDto;
import com.aviraj.user_service.user.entity.User;
import com.aviraj.user_service.common.exception.UserNotFoundException;
import com.aviraj.user_service.user.repository.UserRepository;
import com.aviraj.user_service.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;
    private final UserMapper userMapper;
    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository repository, UserMapper userMapper) {
        this.repository = repository;
        this.userMapper = userMapper;
    }

    public UserResponseDto createUser(UserRequestDto dto) {
        logger.info("creating user with name: {}", dto.getName());
        User user = userMapper.toUser(dto);
        User savedUser = repository.save(user);

        logger.info("User created successfully with id: {}", savedUser.getId());
        //map user to UserResponseDto
        return userMapper.toResponseDto(savedUser);
    }

    public List<UserResponseDto> getAllUsers() {
        return userMapper.toResponseDto(repository.findAll());
    }

    public  UserResponseDto getUserById(Long id){
        User user = repository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found by id: {}", id);
                    return new UserNotFoundException("User not found with given id: " + id);
                });
        return userMapper.toResponseDto(user);
    }

    public UserResponseDto updateUser(Long id, UserRequestDto updatedUser) {
        User user = repository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found with id: {}", id);
                    return new UserNotFoundException("User not found, can't update");
                });

        user.setName(updatedUser.getName());
        user.setEmail(updatedUser.getEmail());

        User savedUser = repository.save(user);

        logger.info("User updated successfully with id: {}",savedUser.getId());
        return userMapper.toResponseDto(savedUser);
    }

    public void deleteUser(Long id){
        User user = repository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found with id: {} can't delete", id);
                    return new UserNotFoundException("User not found, can't delete");
                });
        logger.info("User deleted successfully with id: {}", id);
        repository.delete(user);
    }


}