package com.vegas.user.repository;

import com.vegas.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Ключ профиля = userId, поэтому хватает стандартного findById(userId). */
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
}
