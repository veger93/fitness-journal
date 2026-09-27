package com.vegas.user.mapper;

import com.vegas.user.dto.WeightEntryResponse;
import com.vegas.user.entity.BodyWeightEntry;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WeightMapper {

    WeightEntryResponse toResponse(BodyWeightEntry entry);

    /** Для списка MapStruct сам сгенерирует цикл, вызывая toResponse для каждого элемента. */
    List<WeightEntryResponse> toResponses(List<BodyWeightEntry> entries);
}
