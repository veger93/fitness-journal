package com.vegas.notification.repository;

import com.vegas.notification.entity.ProgramReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProgramReviewRepository extends JpaRepository<ProgramReview, UUID> {

    /** Пересмотр наступил (или наступит завтра — у части людей «сегодня» уже завтра по UTC) */
    List<ProgramReview> findByReviewOnLessThanEqual(LocalDate day);
}
