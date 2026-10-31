package com.vegas.user.service;

import com.vegas.common.model.Gender;
import com.vegas.user.entity.AuthProvider;
import com.vegas.user.entity.ExperienceLevel;
import com.vegas.user.entity.TrainerClient;
import com.vegas.user.entity.User;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.event.OutboxService;
import com.vegas.user.exception.TooManyRequestsException;
import com.vegas.user.repository.TrainerClientRepository;
import com.vegas.user.repository.UserProfileRepository;
import com.vegas.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Демо "посмотреть изнутри" (docs/demo.md, шаг 1).
 *
 * Здесь — только люди: пользователь, профиль, вес тела, у тренера — клиенты и связи с ними.
 * Тренировки генерирует workout-service по событию DemoDataRequested (одно на каждого человека),
 * а рекорды, плато и перегрузку считает аналитика по обычным WorkoutCompleted. Отвечаем токенами сразу:
 * пока генерация идёт, фронт показывает "Готовим ваш дневник…".
 */
@Slf4j
@Service
public class DemoService {

    /** Сценарии людей — их понимает генератор в workout-service */
    static final String ATHLETE = "ATHLETE";
    static final String COACH = "COACH";

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final BodyWeightService bodyWeightService;
    private final TrainerClientRepository trainerClientRepository;
    private final OutboxService outboxService;
    private final AuthService authService;
    private final DemoRateLimiter rateLimiter;
    private final Clock clock;
    private final Duration ttl;
    private final int perIpPerHour;
    private final int maxActive;

    public DemoService(UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       BodyWeightService bodyWeightService,
                       TrainerClientRepository trainerClientRepository,
                       OutboxService outboxService,
                       AuthService authService,
                       DemoRateLimiter rateLimiter,
                       Clock clock,
                       @Value("${app.demo.ttl:24h}") Duration ttl,
                       @Value("${app.demo.per-ip-per-hour:5}") int perIpPerHour,
                       @Value("${app.demo.max-active:200}") int maxActive) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.bodyWeightService = bodyWeightService;
        this.trainerClientRepository = trainerClientRepository;
        this.outboxService = outboxService;
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.ttl = ttl;
        this.perIpPerHour = perIpPerHour;
        this.maxActive = maxActive;
    }

    /**
     * Сначала общий лимит (база), потом лимит IP: иначе отказ "много гостей" съедал бы попытки этого IP.
     * Всё в одной транзакции: упадёт посередине — не останется "полудемо" без профиля или без событий.
     */
    @Transactional
    public AuthTokens start(DemoScenario scenario, String ip) {
        Instant now = clock.instant();
        if (userRepository.countByAuthProviderAndDemoExpiresAtAfter(AuthProvider.DEMO, now) >= maxActive) {
            throw new TooManyRequestsException("Сейчас много гостей — попробуйте через несколько минут");
        }
        if (!rateLimiter.tryAcquire(ip, perIpPerHour, now)) {
            throw new TooManyRequestsException("Слишком много демо подряд — попробуйте через час или зарегистрируйтесь");
        }

        Instant expiresAt = now.plus(ttl);
        User user = scenario == DemoScenario.COACH ? createCoach(expiresAt) : createAthlete(expiresAt);
        log.info("Demo {} started: user {}", scenario, user.getId());
        return authService.issueTokens(user);
    }

    private User createAthlete(Instant expiresAt) {
        User user = person(User.demo("Алексей", expiresAt),
                new Body(Gender.MALE, 1994, 181, ExperienceLevel.INTERMEDIATE, "82.0", "79.2", 10));
        outboxService.demoDataRequested(user.getId(), ATHLETE, null);
        return user;
    }

    /** Тренер и 5 клиентов без аккаунта — у каждого своя метка в "Чей дневник открыть" */
    private User createCoach(Instant expiresAt) {
        User coach = User.demo("Марина", expiresAt);
        coach.setTrainer(true);
        person(coach, new Body(Gender.FEMALE, 1990, 168, ExperienceLevel.ADVANCED, "61.5", "61.0", 6));
        outboxService.demoDataRequested(coach.getId(), COACH, null);

        for (DemoClient c : CLIENTS) {
            User client = person(User.demoClient(c.name(), expiresAt), c.body());
            // клиент у тренера уже несколько месяцев — как и его тренировки
            TrainerClient link = trainerClientRepository.save(new TrainerClient(coach.getId(), client, c.phone(),
                    clock.instant().minus(Duration.ofDays(120))));
            // копии связей в других сервисах: доступ тренера к данным клиента и уведомления тренеру
            outboxService.trainerClientLinked(link);
            // тренировки клиента "записывал" тренер
            outboxService.demoDataRequested(client.getId(), c.scenario(), coach.getId());
        }
        return coach;
    }

    /** Пользователь с заполненным профилем (онбординг пройден) и историей веса за последние ~2 месяца */
    private User person(User user, Body body) {
        userRepository.save(user);
        UserProfile profile = new UserProfile(user);
        profile.setGender(body.gender());
        profile.setBirthYear((short) body.birthYear());
        profile.setHeightCm((short) body.heightCm());
        profile.setExperienceLevel(body.level());
        profile.setOnboardingCompleted(true);
        profileRepository.save(profile);

        // вес плавно меняется от from к to: замер раз в неделю, последний — сегодня (или lastDaysAgo назад)
        LocalDate last = LocalDate.now(clock).minusDays(body.lastDaysAgo());
        BigDecimal from = new BigDecimal(body.weightFrom());
        BigDecimal step = new BigDecimal(body.weightTo()).subtract(from)
                .divide(BigDecimal.valueOf(Math.max(1, body.weighIns() - 1)), 2, RoundingMode.HALF_UP);
        for (int i = 0; i < body.weighIns(); i++) {
            BigDecimal weight = i == body.weighIns() - 1 ? new BigDecimal(body.weightTo()) : from.add(step.multiply(BigDecimal.valueOf(i)));
            bodyWeightService.upsert(user.getId(), weight, last.minusWeeks(body.weighIns() - 1L - i));
        }
        // аналитике — уровень и вес (норма роста на графиках зависит от уровня)
        outboxService.profileUpdated(user.getId());
        return user;
    }

    /** lastDaysAgo — когда последний замер: у "давно не был" он тоже давно, а не сегодня */
    private record Body(Gender gender, int birthYear, int heightCm, ExperienceLevel level,
                        String weightFrom, String weightTo, int weighIns, int lastDaysAgo) {
        Body(Gender gender, int birthYear, int heightCm, ExperienceLevel level, String weightFrom, String weightTo, int weighIns) {
            this(gender, birthYear, heightCm, level, weightFrom, weightTo, weighIns, 0);
        }
    }

    private record DemoClient(String name, String phone, String scenario, Body body) {
    }

    private static final List<DemoClient> CLIENTS = List.of(
            new DemoClient("Иван", "+7 900 111-22-33", "CLIENT_IN_PROGRESS",
                    new Body(Gender.MALE, 1997, 178, ExperienceLevel.BEGINNER, "76.0", "74.5", 6)),
            new DemoClient("Ольга", null, "CLIENT_OVERLOAD",
                    new Body(Gender.FEMALE, 1992, 165, ExperienceLevel.INTERMEDIATE, "63.0", "62.0", 6)),
            new DemoClient("Пётр", "+7 900 444-55-66", "CLIENT_PLATEAU",
                    new Body(Gender.MALE, 1985, 183, ExperienceLevel.INTERMEDIATE, "88.0", "88.5", 6)),
            new DemoClient("Марат", null, "CLIENT_AWAY",
                    new Body(Gender.MALE, 1999, 175, ExperienceLevel.BEGINNER, "70.0", "71.0", 4, 12)),
            new DemoClient("Анна", null, "CLIENT_RECORD",
                    new Body(Gender.FEMALE, 1996, 170, ExperienceLevel.INTERMEDIATE, "59.0", "58.0", 6)));
}
