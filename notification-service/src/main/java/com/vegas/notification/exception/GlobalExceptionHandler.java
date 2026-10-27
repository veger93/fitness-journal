package com.vegas.notification.exception;

import com.vegas.common.dto.ErrorResponse;
import com.vegas.common.web.CommonExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {

    /**
     * Почтовый сервер не ответил (локально — не запущен Mailpit). 503: сервис жив, но зависимость недоступна.
     * Текст — понятный человеку, подробности — в логе.
     */
    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponse> handleMail(MailException ex, HttpServletRequest request) {
        log.error("Mail server error", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Почтовый сервер недоступен — письмо не отправлено", request);
    }
}
