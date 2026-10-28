package com.vegas.workout.dto;

/**
 * Что стало с упражнением после "удалить":
 * DELETED — по нему ничего не было, удалили совсем;
 * ARCHIVED — есть тренировки или комплексы, поэтому только спрятали из каталога (история и графики остались).
 */
public record DeleteExerciseResponse(Result result) {

    public enum Result {
        DELETED,
        ARCHIVED
    }
}
