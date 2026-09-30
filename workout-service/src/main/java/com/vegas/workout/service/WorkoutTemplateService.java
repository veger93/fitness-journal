package com.vegas.workout.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.ForbiddenException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.workout.dto.WorkoutTemplateRequest;
import com.vegas.workout.dto.WorkoutTemplateResponse;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.mapper.WorkoutTemplateMapper;
import com.vegas.workout.repository.ExerciseRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutTemplateService {

    private final WorkoutTemplateRepository templateRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutTemplateMapper templateMapper;

    @Transactional(readOnly = true)
    public List<WorkoutTemplateResponse> list(UUID userId) {
        return templateMapper.toResponses(templateRepository.findAllVisibleTo(userId));
    }

    @Transactional(readOnly = true)
    public WorkoutTemplateResponse getById(UUID userId, UUID templateId) {
        return templateMapper.toResponse(findVisible(userId, templateId));
    }

    @Transactional
    public WorkoutTemplateResponse create(UUID userId, WorkoutTemplateRequest request) {
        String name = request.name().trim();
        ensureNameFree(userId, name, null);
        List<Exercise> exercises = resolveExercises(userId, request.exerciseIds());

        WorkoutTemplate template = WorkoutTemplate.custom(userId, name, request.color(), request.icon(), exercises);
        return templateMapper.toResponse(templateRepository.save(template));
    }

    @Transactional
    public WorkoutTemplateResponse update(UUID userId, UUID templateId, WorkoutTemplateRequest request) {
        WorkoutTemplate template = findOwn(userId, templateId);
        String name = request.name().trim();
        ensureNameFree(userId, name, templateId);
        List<Exercise> exercises = resolveExercises(userId, request.exerciseIds());

        template.update(name, request.color(), request.icon(), exercises);
        return templateMapper.toResponse(template);
    }

    @Transactional
    public void delete(UUID userId, UUID templateId) {
        templateRepository.delete(findOwn(userId, templateId));
    }

    /** Скопировать комплекс (обычно готовый) в "Мои комплексы", чтобы менять под себя. */
    @Transactional
    public WorkoutTemplateResponse copy(UUID userId, UUID templateId) {
        WorkoutTemplate source = findVisible(userId, templateId);
        ensureNameFree(userId, source.getName(), null);
        return templateMapper.toResponse(templateRepository.save(source.copyFor(userId)));
    }

    /**
     * id из запроса -> упражнения В ТОМ ЖЕ ПОРЯДКЕ.
     * findAllById возвращает их в произвольном порядке, поэтому раскладываем в Map по id
     * и собираем список заново, идя по исходным id.
     */
    private List<Exercise> resolveExercises(UUID userId, List<UUID> exerciseIds) {
        if (new HashSet<>(exerciseIds).size() != exerciseIds.size()) {
            throw new BadRequestException("Упражнение не может повторяться в комплексе");
        }
        Map<UUID, Exercise> visible = exerciseRepository.findAllById(exerciseIds).stream()
                .filter(exercise -> exercise.isVisibleTo(userId))
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));

        return exerciseIds.stream()
                .map(id -> {
                    Exercise exercise = visible.get(id);
                    if (exercise == null) {
                        throw new BadRequestException("Упражнение не найдено: " + id);
                    }
                    return exercise;
                })
                .toList();
    }

    private void ensureNameFree(UUID userId, String name, UUID excludeTemplateId) {
        boolean taken = excludeTemplateId == null
                ? templateRepository.existsByOwnerIdAndNameIgnoreCase(userId, name)
                : templateRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(userId, name, excludeTemplateId);
        if (taken) {
            throw new BusinessException("У вас уже есть комплекс «" + name + "»");
        }
    }

    private WorkoutTemplate findVisible(UUID userId, UUID templateId) {
        return templateRepository.findWithExercisesById(templateId)
                .filter(template -> template.isVisibleTo(userId))
                .orElseThrow(() -> new NotFoundException("Комплекс не найден"));
    }

    /** Свой комплекс: чужой -> 404 (как будто его нет), готовый -> 403 (виден, но менять нельзя). */
    private WorkoutTemplate findOwn(UUID userId, UUID templateId) {
        WorkoutTemplate template = findVisible(userId, templateId);
        if (!template.isOwnedBy(userId)) {
            throw new ForbiddenException("Готовые комплексы нельзя изменять — скопируйте его в свои");
        }
        return template;
    }
}
