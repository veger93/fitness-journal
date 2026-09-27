package com.vegas.workout.exception;

import com.vegas.common.web.CommonExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Обработка ошибок workout-service. Пока хватает общих обработчиков из common-lib.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {
}
