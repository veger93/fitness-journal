package com.vegas.user.service;

import com.vegas.common.exception.BusinessException;
import com.vegas.user.dto.AuthResponse;
import com.vegas.user.dto.LoginRequest;
import com.vegas.user.dto.RegisterRequest;
import com.vegas.user.entity.AuthProvider;
import com.vegas.user.entity.User;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.exception.InvalidCredentialsException;
import com.vegas.user.repository.UserProfileRepository;
import com.vegas.user.repository.UserRepository;
import com.vegas.user.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тестируем ТОЛЬКО логику AuthService. Всё, от чего он зависит, заменено моками (Mockito):
 * мы сами говорим, что вернёт репозиторий, и проверяем, что сервис с этим сделал.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks // создаёт AuthService и передаёт в конструктор моки выше
    private AuthService authService;

    // ---------- register ----------

    @Test
    void register_savesUserWithHashedPasswordAndEmptyProfile() {
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateAccessToken(any())).thenReturn("jwt-token");
        when(jwtService.getAccessTokenTtl()).thenReturn(Duration.ofHours(1));

        AuthResponse response = authService.register(new RegisterRequest(" Ivan@Mail.RU ", "password123", "Иван"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ivan@mail.ru");     // email нормализован
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");    // в БД — хеш, не пароль
        assertThat(saved.getValue().getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        verify(userProfileRepository).save(any(UserProfile.class));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
    }

    @Test
    void register_whenEmailTaken_throwsAndSavesNothing() {
        when(userRepository.existsByEmail("ivan@mail.ru")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("IVAN@mail.ru", "password123", "Иван")))
                .isInstanceOf(BusinessException.class);

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    // ---------- login ----------

    @Test
    void login_withCorrectPassword_returnsToken() {
        User user = User.local("ivan@mail.ru", "hashed", "Иван");
        when(userRepository.findByEmail("ivan@mail.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");
        when(jwtService.getAccessTokenTtl()).thenReturn(Duration.ofHours(1));

        AuthResponse response = authService.login(new LoginRequest("Ivan@mail.ru", "password123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        User user = User.local("ivan@mail.ru", "hashed", "Иван");
        when(userRepository.findByEmail("ivan@mail.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ivan@mail.ru", "wrong-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_withUnknownEmail_throwsSameInvalidCredentials() {
        when(userRepository.findByEmail("nobody@mail.ru")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@mail.ru", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_forGoogleUserWithoutPassword_throwsInvalidCredentials() {
        User googleUser = User.oauth(AuthProvider.GOOGLE, "google-123", "ivan@gmail.com", "Иван");
        when(userRepository.findByEmail("ivan@gmail.com")).thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ivan@gmail.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
