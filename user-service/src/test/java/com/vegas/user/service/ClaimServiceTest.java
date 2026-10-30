package com.vegas.user.service;

import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.user.dto.AuthResponse;
import com.vegas.user.dto.ClaimLinkResponse;
import com.vegas.user.dto.ClaimRequest;
import com.vegas.user.entity.AuthProvider;
import com.vegas.user.entity.ClaimToken;
import com.vegas.user.entity.TrainerClient;
import com.vegas.user.entity.User;
import com.vegas.user.event.OutboxService;
import com.vegas.user.repository.ClaimTokenRepository;
import com.vegas.user.repository.TrainerClientRepository;
import com.vegas.user.repository.UserRepository;
import com.vegas.user.security.SecureTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ClaimTokenRepository claimTokenRepository;
    @Mock
    private TrainerClientRepository trainerClientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OutboxService outboxService;
    @Mock
    private AuthService authService;
    @Mock
    private EmailCodeService emailCodeService;

    private ClaimService service;
    private final UUID trainerId = UUID.randomUUID();
    private User client;
    private TrainerClient link;

    @BeforeEach
    void setUp() {
        service = new ClaimService(claimTokenRepository, trainerClientRepository, userRepository, passwordEncoder,
                outboxService, authService, emailCodeService, CLOCK);
        client = User.managed("Иван");
        ReflectionTestUtils.setField(client, "id", UUID.randomUUID());
        link = new TrainerClient(trainerId, client, null, NOW.minusSeconds(86_400));
    }

    @Test
    void createLink_storesOnlyHash_andRevokesOldLinks() {
        when(trainerClientRepository.findByTrainerIdAndClient_IdAndEndedAtIsNull(trainerId, client.getId()))
                .thenReturn(Optional.of(link));

        ClaimLinkResponse response = service.createLink(trainerId, client.getId());

        verify(claimTokenRepository).deleteUnusedByClientId(client.getId());
        ArgumentCaptor<ClaimToken> saved = ArgumentCaptor.forClass(ClaimToken.class);
        verify(claimTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(SecureTokens.sha256(response.token()));
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(response.token());
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(ClaimService.LINK_TTL));
    }

    @Test
    void createLink_clientAlreadyHasAccount_rejected() {
        User own = User.local("ivan@mail.ru", "hash", "Иван");
        ReflectionTestUtils.setField(own, "id", UUID.randomUUID());
        when(trainerClientRepository.findByTrainerIdAndClient_IdAndEndedAtIsNull(trainerId, own.getId()))
                .thenReturn(Optional.of(new TrainerClient(trainerId, own, null, NOW)));

        assertThatThrownBy(() -> service.createLink(trainerId, own.getId())).isInstanceOf(BusinessException.class);
    }

    @Test
    void claim_givesLoginToSameAccount_andKeepsTrainer() {
        String raw = givenValidToken();
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt");
        AuthTokens tokens = new AuthTokens(AuthResponse.bearer("jwt", 900, client.getId()), "refresh");
        when(authService.issueTokens(client)).thenReturn(tokens);

        AuthTokens result = service.claim(raw, new ClaimRequest(" Ivan@Mail.ru ", "secret123", true, true));

        assertThat(result).isSameAs(tokens);
        // тот же пользователь (тот же id) — данные никуда не переносятся
        assertThat(client.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(client.getEmail()).isEqualTo("ivan@mail.ru");
        assertThat(client.getDataConsentAt()).isEqualTo(NOW);
        verify(outboxService).userRegistered(client);
        // код подтверждения — в приветственном письме: "тренер Сергей передал вам дневник"
        verify(emailCodeService).sendVerificationOnClaim(client, "Сергей", true);
        verify(outboxService, never()).trainerClientUnlinked(any());
    }

    @Test
    void claim_goingAlone_endsTrainerLink() {
        String raw = givenValidToken();
        when(passwordEncoder.encode(any())).thenReturn("bcrypt");
        when(trainerClientRepository.findByClient_IdAndEndedAtIsNull(client.getId())).thenReturn(Optional.of(link));

        service.claim(raw, new ClaimRequest("ivan@mail.ru", "secret123", false, true));

        assertThat(link.getEndedAt()).isEqualTo(NOW);
        verify(outboxService).trainerClientUnlinked(link);
    }

    @Test
    void claim_expiredLink_notFound() {
        String raw = "expired-token";
        ClaimToken token = new ClaimToken(client.getId(), trainerId, SecureTokens.sha256(raw), NOW.minusSeconds(1));
        when(claimTokenRepository.findByTokenHash(SecureTokens.sha256(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.claim(raw, new ClaimRequest("ivan@mail.ru", "secret123", true, true)))
                .isInstanceOf(NotFoundException.class);
        assertThat(client.isManaged()).isTrue();
    }

    @Test
    void claim_emailTaken_rejected() {
        String raw = givenValidToken();
        when(userRepository.existsByEmail("ivan@mail.ru")).thenReturn(true);

        assertThatThrownBy(() -> service.claim(raw, new ClaimRequest("ivan@mail.ru", "secret123", true, true)))
                .isInstanceOf(BusinessException.class);
        assertThat(client.isManaged()).isTrue();
    }

    private String givenValidToken() {
        String raw = SecureTokens.generate();
        ClaimToken token = new ClaimToken(client.getId(), trainerId, SecureTokens.sha256(raw), NOW.plusSeconds(3600));
        when(claimTokenRepository.findByTokenHash(SecureTokens.sha256(raw))).thenReturn(Optional.of(token));
        when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));
        // lenient: тренер нужен только в успешной передаче (имя для письма), в тестах с отказом — нет
        User trainer = User.local("coach@mail.ru", "hash", "Сергей");
        lenient().when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        return raw;
    }
}
