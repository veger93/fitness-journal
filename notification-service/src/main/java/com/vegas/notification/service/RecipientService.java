package com.vegas.notification.service;

import com.vegas.common.event.UserRegisteredEvent;
import com.vegas.notification.entity.Recipient;
import com.vegas.notification.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Локальная копия email и имени пользователя. Своя копия = не зависим от доступности user-service. */
@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientRepository recipientRepository;

    /** Upsert: повторное событие не падает, а просто обновляет данные. */
    @Transactional
    public void register(UserRegisteredEvent event) {
        recipientRepository.findById(event.userId()).ifPresentOrElse(
                existing -> existing.update(event.email(), event.displayName()),
                () -> recipientRepository.save(new Recipient(event.userId(), event.email(), event.displayName())));
    }
}
