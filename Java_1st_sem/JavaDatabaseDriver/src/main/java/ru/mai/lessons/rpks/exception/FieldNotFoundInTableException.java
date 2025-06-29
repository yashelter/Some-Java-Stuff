package ru.mai.lessons.rpks.exception;

/**
 * Выбрасывается, если в команде SELECT передано поле, которое отсутствует в таблице.
 */
public class FieldNotFoundInTableException extends Exception {
  public FieldNotFoundInTableException(String message) {
    super(message);
  }
}
