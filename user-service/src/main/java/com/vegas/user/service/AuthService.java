package com.vegas.user.service;

import com.vegas.common.exception.BusinessException;
import com.vegas.user.dto.AuthResponse;
import com.vegas.user.dto.LoginRequest;
import com.vegas.user.dto.RegisterRequest;
import com.vegas.user.entity.User;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.exception.InvalidCredentialsException;
import com.vegas.user.repository.UserProfileRepository;
import com.vegas.user.repository.UserRepository;
import com.vegas.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Регистрация и вход по email + паролю.
 * @RequiredArgsConstructor (Lombok) создаёт конструктор для final-полей -> Spring внедряет зависимости через него.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * @Transactional: пользователь и его профиль сохраняются вместе.
     * Если что-то упадёт посередине — откатится всё, "пользователя без профиля" не останется.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Пользователь с таким email уже зарегистрирован");
        }

        User user = User.local(email, passwordEncoder.encode(request.password()), request.displayName());
        userRepository.save(user);
        // пустой профиль сразу: онбординг потом только обновляет его
        userProfileRepository.save(new UserProfile(user));

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email()))
                // у Google-пользователя пароля нет -> войти по паролю нельзя
                .filter(u -> u.getPasswordHash() != null)
                // matches сам хеширует введённый пароль с солью из хеша и сравнивает
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.bearer(
                jwtService.generateAccessToken(user),
                jwtService.getAccessTokenTtl().toSeconds(),
                user.getId()
        );
    }
}
