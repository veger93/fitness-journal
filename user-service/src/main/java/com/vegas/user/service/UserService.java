package com.vegas.user.service;

import com.vegas.common.exception.NotFoundException;
import com.vegas.user.dto.UserResponse;
import com.vegas.user.mapper.UserMapper;
import com.vegas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                // токен валиден, а пользователя уже нет (удалили) -> 404
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }
}
