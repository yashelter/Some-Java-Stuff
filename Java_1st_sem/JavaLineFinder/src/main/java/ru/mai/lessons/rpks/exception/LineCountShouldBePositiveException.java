package ru.mai.lessons.rpks.exception;

/**
 * Выбрасывается, если в качестве количества строк на вход методу
 * {@link ru.mai.lessons.rpks.impl.LineFinder#find(String, String, String, int)} передано
 * отрицательное значение.
 */
public class LineCountShouldBePositiveException extends Exception {
  public LineCountShouldBePositiveException(String message) {
    super(message);
  }
}
