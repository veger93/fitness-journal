package com.vegas.user.mapper;

import com.vegas.user.dto.ProfileResponse;
import com.vegas.user.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

/**
 * Пример маппинга из ДВУХ источников: поля профиля + отдельно посчитанный текущий вес.
 * Поля с совпадающими именами MapStruct берёт из profile, currentWeightKg — из второго параметра.
 */
@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(target = "currentWeightKg", source = "currentWeightKg")
    ProfileResponse toResponse(UserProfile profile, BigDecimal currentWeightKg);
}
