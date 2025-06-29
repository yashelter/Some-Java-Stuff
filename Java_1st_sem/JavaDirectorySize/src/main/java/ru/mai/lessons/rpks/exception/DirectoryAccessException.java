package ru.mai.lessons.rpks.exception;

/**
 * Данное исключение выбрасывается, если в функцию
 * {@link ru.mai.lessons.rpks.impl.DirectorySizeChecker#checkSize(String)} передано название
 * несуществующей директории или если к директории нет доступа.
 */
public class DirectoryAccessException extends Exception {

  public DirectoryAccessException(String message) {
    super(message);
  }
}
