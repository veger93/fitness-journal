package com.vegas.workout.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.workout.dto.WeeklyPlanRequest;
import com.vegas.workout.dto.WeeklyPlanResponse;
import com.vegas.workout.entity.WeeklyPlanDay;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.mapper.WorkoutTemplateMapper;
import com.vegas.workout.repository.WeeklyPlanDayRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyPlanServiceTest {

    @Mock
    private WeeklyPlanDayRepository planRepository;
    @Mock
    private WorkoutTemplateRepository templateRepository;

    private final WorkoutTemplateMapper templateMapper = Mappers.getMapper(WorkoutTemplateMapper.class);
    private WeeklyPlanService service;

    private final UUID me = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WeeklyPlanService(planRepository, templateRepository, templateMapper);
    }

    @Test
    void get_alwaysSevenDaysFromMonday_restDaysAreNull() {
        WorkoutTemplate upper = template(me, "Верх");
        when(planRepository.findByUserId(me)).thenReturn(List.of(new WeeklyPlanDay(me, DayOfWeek.WEDNESDAY, upper)));

        WeeklyPlanResponse plan = service.get(me);

        assertThat(plan.days()).extracting(WeeklyPlanResponse.Day::dayOfWeek).containsExactly(DayOfWeek.values());
        assertThat(plan.days().get(2).template().name()).isEqualTo("Верх");
        assertThat(plan.days().get(0).template()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void replace_deletesOldPlanFirst_skipsRestDays() {
        WorkoutTemplate upper = template(me, "Верх");
        when(templateRepository.findWithExercisesById(upper.getId())).thenReturn(Optional.of(upper));
        when(planRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WeeklyPlanResponse plan = service.replace(me, new WeeklyPlanRequest(List.of(
                new WeeklyPlanRequest.Day(DayOfWeek.MONDAY, upper.getId()),
                new WeeklyPlanRequest.Day(DayOfWeek.TUESDAY, null))));

        verify(planRepository).deleteAllByUserId(me);
        ArgumentCaptor<List<WeeklyPlanDay>> saved = ArgumentCaptor.forClass(List.class);
        verify(planRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(WeeklyPlanDay::getDayOfWeek).containsExactly(DayOfWeek.MONDAY);
        assertThat(plan.days().get(0).template().name()).isEqualTo("Верх");
    }

    @Test
    void replace_sameDayTwice_isRejected_beforeDeleting() {
        UUID id = UUID.randomUUID();
        WeeklyPlanRequest request = new WeeklyPlanRequest(List.of(
                new WeeklyPlanRequest.Day(DayOfWeek.MONDAY, id),
                new WeeklyPlanRequest.Day(DayOfWeek.MONDAY, id)));

        assertThatThrownBy(() -> service.replace(me, request)).isInstanceOf(BadRequestException.class);
        verify(planRepository, never()).deleteAllByUserId(any());
    }

    @Test
    void replace_someoneElsesTemplate_isNotFound() {
        WorkoutTemplate foreign = template(UUID.randomUUID(), "Чужой");
        when(templateRepository.findWithExercisesById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.replace(me, new WeeklyPlanRequest(List.of(
                new WeeklyPlanRequest.Day(DayOfWeek.FRIDAY, foreign.getId())))))
                .isInstanceOf(NotFoundException.class);
    }

    private static WorkoutTemplate template(UUID owner, String name) {
        WorkoutTemplate template = WorkoutTemplate.custom(owner, name, "blue", null, List.of());
        ReflectionTestUtils.setField(template, "id", UUID.randomUUID());
        return template;
    }
}
