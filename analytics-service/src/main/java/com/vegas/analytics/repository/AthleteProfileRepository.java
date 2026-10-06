package com.vegas.analytics.repository;

import com.vegas.analytics.entity.AthleteProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AthleteProfileRepository extends JpaRepository<AthleteProfile, UUID> {
}
