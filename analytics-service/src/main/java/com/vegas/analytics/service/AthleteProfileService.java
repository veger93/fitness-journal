package com.vegas.analytics.service;

import com.vegas.analytics.entity.AthleteProfile;
import com.vegas.analytics.repository.AthleteProfileRepository;
import com.vegas.common.event.UserProfileUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AthleteProfileService {

    private final AthleteProfileRepository profileRepository;

    /** Идемпотентно: повтор того же события ничего не меняет, более старое — игнорируется. */
    @Transactional
    public void apply(UserProfileUpdatedEvent event) {
        profileRepository.findById(event.userId()).ifPresentOrElse(
                existing -> {
                    if (!existing.applyIfNewer(event)) {
                        log.debug("Stale profile event for user {} skipped", event.userId());
                    }
                },
                () -> profileRepository.save(AthleteProfile.from(event))
        );
    }
}
