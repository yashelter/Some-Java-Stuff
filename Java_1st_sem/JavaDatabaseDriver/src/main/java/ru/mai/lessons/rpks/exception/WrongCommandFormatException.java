package ru.mai.lessons.rpks.exception;

/**
 * Выбрасывается, если формат команды не соответствует требованиям.
 */
public class WrongCommandFormatException extends Exception {
  public WrongCommandFormatException(String message) {
    super(message);
  }
}
