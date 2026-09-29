package com.vegas.workout.service;

import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.ForbiddenException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.common.model.BodyPart;
import com.vegas.workout.dto.ExerciseRequest;
import com.vegas.workout.dto.ExerciseResponse;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.mapper.ExerciseMapper;
import com.vegas.workout.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.vegas.workout.repository.ExerciseSpecifications.hasBodyPart;
import static com.vegas.workout.repository.ExerciseSpecifications.nameContains;
import static com.vegas.workout.repository.ExerciseSpecifications.visibleTo;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseMapper exerciseMapper;

    /** Каталог: системные + свои, с необязательными фильтрами. Каталог небольшой — без пагинации. */
    @Transactional(readOnly = true)
    public List<ExerciseResponse> search(UUID userId, BodyPart bodyPart, String search) {
        var spec = visibleTo(userId)
                .and(hasBodyPart(bodyPart))
                .and(nameContains(search));
        return exerciseMapper.toResponses(exerciseRepository.findAll(spec, Sort.by("name")));
    }

    @Transactional(readOnly = true)
    public ExerciseResponse getById(UUID userId, UUID exerciseId) {
        return exerciseMapper.toResponse(findVisible(userId, exerciseId));
    }

    @Transactional
    public ExerciseResponse create(UUID userId, ExerciseRequest request) {
        String name = request.name().trim();
        if (exerciseRepository.existsByOwnerIdAndNameIgnoreCase(userId, name)) {
            throw new BusinessException("У вас уже есть упражнение «" + name + "»");
        }
        Exercise exercise = Exercise.custom(userId, name, request.bodyPart(), request.equipment(),
                request.trackingType(), request.icon(), request.note());
        return exerciseMapper.toResponse(exerciseRepository.save(exercise));
    }

    @Transactional
    public ExerciseResponse update(UUID userId, UUID exerciseId, ExerciseRequest request) {
        Exercise exercise = findVisible(userId, exerciseId);
        if (!exercise.isOwnedBy(userId)) {
            // системное: видеть можно, менять нельзя -> 403
            throw new ForbiddenException("Системные упражнения нельзя изменять");
        }
        String name = request.name().trim();
        if (exerciseRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(userId, name, exerciseId)) {
            throw new BusinessException("У вас уже есть упражнение «" + name + "»");
        }
        exercise.update(name, request.bodyPart(), request.equipment(),
                request.trackingType(), request.icon(), request.note());
        return exerciseMapper.toResponse(exercise); // save() не нужен — dirty checking
    }

    /**
     * Чужое своё упражнение -> 404, а не 403: не подтверждаем даже сам факт, что такой id существует.
     */
    private Exercise findVisible(UUID userId, UUID exerciseId) {
        return exerciseRepository.findWithMusclesById(exerciseId)
                .filter(exercise -> !exercise.isCustom() || exercise.isOwnedBy(userId))
                .orElseThrow(() -> new NotFoundException("Упражнение не найдено"));
    }
}
