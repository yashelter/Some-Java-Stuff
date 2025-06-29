package ru.mai.lessons.rpks.exception;

/**
 * Данное исключение выбрасывается, если указанную сумму невозможно разменять с помощью указанного
 * размена.
 */
public class ExchangeIsImpossibleException extends Exception {

  public ExchangeIsImpossibleException(String message) {
    super(message);
  }
}
