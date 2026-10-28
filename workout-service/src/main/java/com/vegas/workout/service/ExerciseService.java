package com.vegas.workout.service;

import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.ForbiddenException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.common.model.BodyPart;
import com.vegas.workout.dto.DeleteExerciseResponse;
import com.vegas.workout.dto.ExerciseRequest;
import com.vegas.workout.dto.ExerciseResponse;
import com.vegas.workout.dto.TechniqueResponse;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.mapper.ExerciseMapper;
import com.vegas.workout.repository.ExerciseRepository;
import com.vegas.workout.repository.ExerciseTechniqueRepository;
import com.vegas.workout.repository.WorkoutExerciseRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.vegas.workout.repository.ExerciseSpecifications.hasBodyPart;
import static com.vegas.workout.repository.ExerciseSpecifications.nameContains;
import static com.vegas.workout.repository.ExerciseSpecifications.notArchived;
import static com.vegas.workout.repository.ExerciseSpecifications.visibleTo;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseTechniqueRepository techniqueRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutTemplateRepository templateRepository;
    private final ExerciseMapper exerciseMapper;
    private final Clock clock;

    /** Каталог: системные + свои, с необязательными фильтрами. Каталог небольшой — без пагинации. */
    @Transactional(readOnly = true)
    public List<ExerciseResponse> search(UUID userId, BodyPart bodyPart, String search) {
        var spec = visibleTo(userId)
                .and(notArchived())
                .and(hasBodyPart(bodyPart))
                .and(nameContains(search));
        return exerciseMapper.toResponses(exerciseRepository.findAll(spec, Sort.by("name")));
    }

    @Transactional(readOnly = true)
    public ExerciseResponse getById(UUID userId, UUID exerciseId) {
        return exerciseMapper.toResponse(findVisible(userId, exerciseId));
    }

    /**
     * Техника выполнения. Сначала проверяем, что упражнение видно пользователю (чужое своё -> 404),
     * потом ищем технику. Её может не быть (свои упражнения) — это не ошибка, поэтому Optional, а не исключение.
     */
    @Transactional(readOnly = true)
    public Optional<TechniqueResponse> getTechnique(UUID userId, UUID exerciseId) {
        Exercise exercise = findVisible(userId, exerciseId);
        return techniqueRepository.findById(exercise.getId()).map(exerciseMapper::toTechniqueResponse);
    }

    /**
     * Копию упражнения из каталога создать нельзя: "Жим лёжа" системный и "жим лёжа" свой —
     * это два разных упражнения для аналитики, и прогресс разъехался бы по двум графикам.
     */
    @Transactional
    public ExerciseResponse create(UUID userId, ExerciseRequest request) {
        String name = request.name().trim();
        if (exerciseRepository.existsByOwnerIdAndNameIgnoreCaseAndArchivedAtIsNull(userId, name)) {
            throw new BusinessException("У вас уже есть упражнение «" + name + "»");
        }
        if (exerciseRepository.existsByOwnerIdIsNullAndNameIgnoreCase(name)) {
            throw new BusinessException("«" + name + "» уже есть в каталоге — найдите его поиском");
        }
        // когда-то удалили, а теперь создают снова — возвращаем то же упражнение, чтобы история не разделилась
        var archived = exerciseRepository.findFirstByOwnerIdAndNameIgnoreCaseAndArchivedAtIsNotNull(userId, name);
        if (archived.isPresent()) {
            Exercise exercise = archived.get();
            exercise.restore();
            exercise.update(name, request.bodyPart(), request.equipment(),
                    request.trackingType(), request.icon(), request.note());
            return exerciseMapper.toResponse(exercise);
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
        if (exerciseRepository.existsByOwnerIdIsNullAndNameIgnoreCase(name)) {
            throw new BusinessException("«" + name + "» уже есть в каталоге — найдите его поиском");
        }
        exercise.update(name, request.bodyPart(), request.equipment(),
                request.trackingType(), request.icon(), request.note());
        return exerciseMapper.toResponse(exercise); // save() не нужен — dirty checking
    }

    /**
     * Удалить своё упражнение. Есть тренировки или комплексы с ним — не удаляем, а прячем (archive):
     * иначе пропали бы история и графики, а внешние ключи workout_exercises и комплексов и не дали бы удалить.
     * Ничего нет — удаляем совсем, мышцы и прочее уйдут каскадом (ON DELETE CASCADE).
     */
    @Transactional
    public DeleteExerciseResponse delete(UUID userId, UUID exerciseId) {
        Exercise exercise = findVisible(userId, exerciseId);
        if (!exercise.isOwnedBy(userId)) {
            throw new ForbiddenException("Системные упражнения удалить нельзя");
        }
        boolean used = workoutExerciseRepository.existsByExerciseId(exerciseId)
                || templateRepository.existsByExercisesId(exerciseId);
        if (used) {
            if (!exercise.isArchived()) {
                exercise.archive(clock.instant());
            }
            return new DeleteExerciseResponse(DeleteExerciseResponse.Result.ARCHIVED);
        }
        exerciseRepository.delete(exercise);
        return new DeleteExerciseResponse(DeleteExerciseResponse.Result.DELETED);
    }

    /**
     * Чужое своё упражнение -> 404, а не 403: не подтверждаем даже сам факт, что такой id существует.
     */
    private Exercise findVisible(UUID userId, UUID exerciseId) {
        return exerciseRepository.findWithMusclesById(exerciseId)
                .filter(exercise -> exercise.isVisibleTo(userId))
                .orElseThrow(() -> new NotFoundException("Упражнение не найдено"));
    }
}
