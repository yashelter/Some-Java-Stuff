package ru.mai.lessons.rpks.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ControllerExceptionHandler {

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ExceptionErrorMessage> handle(Exception ex) {
    return ResponseEntity
        .unprocessableEntity()
        .body(
            ExceptionErrorMessage
                .builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .error(ex.getMessage())
                .build()
        );
  }
}
