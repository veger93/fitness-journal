package com.vegas.user.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.user.entity.EmailCode;
import com.vegas.user.entity.EmailCode.Purpose;
import com.vegas.user.entity.RefreshToken;
import com.vegas.user.entity.User;
import com.vegas.user.event.OutboxService;
import com.vegas.user.mapper.UserMapper;
import com.vegas.user.repository.EmailCodeRepository;
import com.vegas.user.repository.RefreshTokenRepository;
import com.vegas.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailCodeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");

    @Mock
    private EmailCodeRepository codeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OutboxService outboxService;

    private EmailCodeService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new EmailCodeService(codeRepository, userRepository, refreshTokenRepository, passwordEncoder,
                outboxService, Mappers.getMapper(UserMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));
        user = User.local("ivan@mail.ru", "hash", "Иван");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    }

    @Test
    void signUp_sendsSixDigitCode_storesOnlyHash() {
        service.sendVerificationOnSignUp(user);

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(outboxService).emailCodeRequested(eq(user), code.capture(), eq("VERIFY_EMAIL"), any(), isNull(), isNull());
        assertThat(code.getValue()).matches("\\d{6}");
        ArgumentCaptor<EmailCode> saved = ArgumentCaptor.forClass(EmailCode.class);
        verify(codeRepository).save(saved.capture());
        assertThat(saved.getValue().getCodeHash()).isEqualTo(EmailCodeService.hash(user.getId(), code.getValue()));
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(EmailCodeService.CODE_TTL));
    }

    @Test
    void confirm_rightCode_verifiesEmail() {
        givenCode(Purpose.VERIFY_EMAIL, "123456", NOW.minusSeconds(30));

        assertThat(service.confirmEmail(user.getId(), "123456").emailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    void confirm_wrongCode_countsAttempt_andAfterFiveCodeDies() {
        EmailCode code = givenCode(Purpose.VERIFY_EMAIL, "123456", NOW.minusSeconds(30));

        assertThatThrownBy(() -> service.confirmEmail(user.getId(), "000000"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Осталось попыток: 4");
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.confirmEmail(user.getId(), "000000")).isInstanceOf(BadRequestException.class);
        }
        // верный код после 5 ошибок уже не помогает — перебор миллиона вариантов невозможен
        assertThatThrownBy(() -> service.confirmEmail(user.getId(), "123456"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("устарел");
        assertThat(code.getAttempts()).isEqualTo((short) 5);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void confirm_codeForOldEmail_doesNotVerifyNewOne() {
        givenCode(Purpose.VERIFY_EMAIL, "123456", NOW.minusSeconds(30));
        user.changeEmail("new@mail.ru");

        assertThatThrownBy(() -> service.confirmEmail(user.getId(), "123456")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void resend_tooSoon_rejected() {
        givenCode(Purpose.VERIFY_EMAIL, "123456", NOW.minusSeconds(20));

        assertThatThrownBy(() -> service.resendVerification(user.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("через 40 сек");
    }

    @Test
    void forgot_unverifiedEmail_noCodeSent_butNoErrorEither() {
        when(userRepository.findByEmail("ivan@mail.ru")).thenReturn(Optional.of(user));

        service.requestPasswordReset("Ivan@Mail.ru");

        verify(outboxService, never()).emailCodeRequested(any(), any(), any(), any(), any(), any());
    }

    @Test
    void reset_rightCode_changesPassword_andLogsOutEverywhere() {
        user.markEmailVerified(NOW.minusSeconds(3600));
        when(userRepository.findByEmail("ivan@mail.ru")).thenReturn(Optional.of(user));
        givenCode(Purpose.RESET_PASSWORD, "654321", NOW.minusSeconds(60));
        when(passwordEncoder.encode("newpass123")).thenReturn("new-hash");

        service.resetPassword("ivan@mail.ru", "654321", "newpass123");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenRepository).revokeAllOfUser(user.getId(), RefreshToken.RevokeReason.PASSWORD_RESET, NOW);
    }

    private EmailCode givenCode(Purpose purpose, String code, Instant createdAt) {
        EmailCode emailCode = new EmailCode(user.getId(), purpose, user.getEmail(),
                EmailCodeService.hash(user.getId(), code), createdAt, createdAt.plus(EmailCodeService.CODE_TTL));
        // lenient: в сбросе пароля пользователя ищут по email, а не по id
        lenient().when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(codeRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose))
                .thenReturn(Optional.of(emailCode));
        return emailCode;
    }
}
