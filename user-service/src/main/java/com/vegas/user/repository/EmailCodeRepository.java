package com.vegas.user.repository;

import com.vegas.user.entity.EmailCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailCodeRepository extends JpaRepository<EmailCode, UUID> {

    /** Последний отправленный код: действует только он (новый код отменяет старые) */
    Optional<EmailCode> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, EmailCode.Purpose purpose);
}
