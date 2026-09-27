package com.vegas.user.mapper;

import com.vegas.user.dto.UserResponse;
import com.vegas.user.entity.User;
import org.mapstruct.Mapper;

/**
 * MapStruct на этапе КОМПИЛЯЦИИ генерирует класс UserMapperImpl
 * (посмотреть: build/generated/sources/annotationProcessor). Поля сопоставляются по именам.
 * componentModel = "spring" -> реализация становится бином, её можно внедрять в сервисы.
 * Поля, которых нет в UserResponse (passwordHash и т.д.), просто не копируются.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
