package ru.mai.lessons.rpks.exception;

/**
 * Данное исключение выбрасывается при ошибках работы с файлом.
 */
public class WrongFilenameException extends Exception {

  public WrongFilenameException(String message) {
    super(message);
  }
}
